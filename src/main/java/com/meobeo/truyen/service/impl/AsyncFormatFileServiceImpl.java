package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.request.story.FormatFileRequest;
import com.meobeo.truyen.domain.response.story.FormatFileResponse;
import com.meobeo.truyen.service.interfaces.AsyncFormatFileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncFormatFileServiceImpl implements AsyncFormatFileService {

    // Cache để lưu trạng thái các job đang chạy
    private final Map<String, FormatFileResponse> formatJobs = new ConcurrentHashMap<>();

    // Map để lưu userId của từng job
    private final Map<String, Long> jobUserMap = new ConcurrentHashMap<>();

    // Flag để hủy job
    private final Map<String, AtomicBoolean> cancelFlags = new ConcurrentHashMap<>();

    // Thư mục lưu file đã format
    private static final String FORMATTED_FILES_DIR = "formatted_files";

    // Pattern cho watermark
    private static final Pattern WATERMARK_PATTERN = Pattern.compile(
            ".*(DTV-EBOOK|EBOOK|VUILEN|TVE-4U|vietphrase|TruyenYY|TruyenFull|TruyenHay|TruyenKinhDien|TruyenTienHiep|TruyenNgan|TruyenDai|TruyenHot|TruyenMoi|TruyenHay|TruyenKinhDien|TruyenTienHiep|TruyenNgan|TruyenDai|TruyenHot|TruyenMoi).*",
            Pattern.CASE_INSENSITIVE);

    // Pattern cho ký tự đặc biệt ở đầu dòng (bao gồm dấu gạch đầu dòng)
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(
            "^[\\s]*[-–—•*_=+~`!@#$%^&*()\\[\\]{}|\\\\:;\"'<>?,./]+.*$");

    // Pattern cho dòng chỉ chứa ký tự đặc biệt
    private static final Pattern SPECIAL_ONLY_PATTERN = Pattern.compile(
            "^[\\s]*[\\*\\-\\_\\=\\+\\~\\`\\!\\@\\#\\$\\%\\^\\&\\*\\(\\)\\[\\]\\{\\}\\|\\\\\\:\\;\\\"\\'\\<\\>\\?\\,\\.\\/]+[\\s]*$");

    // Pattern cho dòng bắt đầu bằng dấu gạch đầu dòng (cần loại bỏ)
    private static final Pattern DASH_START_PATTERN = Pattern.compile(
            "^[\\s]*[-–—]+[\\s]*(.*)$");

    // Pattern cho dấu câu
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile(
            "\\s*([.,!?;:])\\s*");

    @Async("formatFileExecutor")
    @Override
    public void processFormatFileAsync(MultipartFile txtFile, FormatFileRequest request, Long userId, String jobId) {
        log.info("=== BẮT ĐẦU processFormatFileAsync ===");
        log.info("Thread async: {}", Thread.currentThread().getName());
        log.info("JobId: {}", jobId);

        FormatFileResponse response = formatJobs.get(jobId);
        AtomicBoolean cancelFlag = cancelFlags.get(jobId);

        if (response == null || cancelFlag == null) {
            log.error("Không tìm thấy job hoặc cancel flag: jobId={}", jobId);
            return;
        }

        try {
            // Tạo thư mục lưu file nếu chưa có
            createFormattedFilesDirectory();

            // Cập nhật trạng thái bắt đầu xử lý
            response.setStatus("PROCESSING");
            response.setMessage("Đang đọc và phân tích file TXT...");
            log.info("Bắt đầu xử lý format file TXT: jobId={}", jobId);

            // Đọc file và đếm số dòng
            List<String> originalLines = readFileLines(txtFile, response, cancelFlag);

            if (cancelFlag.get()) {
                response.setStatus("CANCELLED");
                response.setMessage("Format đã bị hủy");
                log.info("Job bị hủy: jobId={}", jobId);
                return;
            }

            response.setTotalLines(originalLines.size());
            response.setOriginalLineCount(originalLines.size());
            response.setOriginalFileSize(txtFile.getSize());
            response.setMessage("Đã đọc " + originalLines.size() + " dòng. Bắt đầu format...");

            // Format từng dòng
            List<String> formattedLines = formatLines(originalLines, request, response, cancelFlag);

            if (cancelFlag.get()) {
                response.setStatus("CANCELLED");
                response.setMessage("Format đã bị hủy");
                log.info("Job bị hủy trong quá trình format: jobId={}", jobId);
                return;
            }

            // Lưu file đã format
            String formattedFileName = saveFormattedFile(formattedLines, request.getOriginalFileName(), jobId,
                    response);

            if (cancelFlag.get()) {
                response.setStatus("CANCELLED");
                response.setMessage("Format đã bị hủy");
                return;
            }

            // Hoàn thành
            response.setStatus("COMPLETED");
            response.setEndTime(LocalDateTime.now());
            response.setFormattedFileName(formattedFileName);
            response.setFormattedLineCount(formattedLines.size());
            response.setFormattedFileSize(calculateFileSize(formattedLines));
            response.setDownloadUrl("/api/v1/format/download/" + jobId);
            response.setMessage("Format hoàn thành. File đã sẵn sàng để download.");

            log.info("Format hoàn thành: jobId={}, originalLines={}, formattedLines={}",
                    jobId, originalLines.size(), formattedLines.size());

        } catch (Exception e) {
            log.error("Lỗi format file: jobId={}, error={}", jobId, e.getMessage(), e);
            response.setStatus("FAILED");
            response.setEndTime(LocalDateTime.now());
            response.addError("Lỗi format: " + e.getMessage());
            response.setMessage("Format thất bại: " + e.getMessage());
        } finally {
            // Cleanup
            cancelFlags.remove(jobId);
            jobUserMap.remove(jobId);
            log.info("Hoàn thành job format: jobId={}, status={}", jobId, response.getStatus());
        }
    }

    /**
     * Tạo thư mục lưu file đã format
     */
    private void createFormattedFilesDirectory() throws IOException {
        Path dir = Paths.get(FORMATTED_FILES_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    /**
     * Đọc file và trả về danh sách dòng
     */
    private List<String> readFileLines(MultipartFile file, FormatFileResponse response, AtomicBoolean cancelFlag)
            throws IOException {
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (cancelFlag.get()) {
                    break;
                }
                lines.add(line);
            }
        }

        return lines;
    }

    /**
     * Format từng dòng theo yêu cầu
     */
    private List<String> formatLines(List<String> originalLines, FormatFileRequest request,
            FormatFileResponse response, AtomicBoolean cancelFlag) {
        List<String> formattedLines = new ArrayList<>();
        boolean previousLineEmpty = false;

        for (int i = 0; i < originalLines.size(); i++) {
            if (cancelFlag.get()) {
                break;
            }

            String line = originalLines.get(i);
            String formattedLine = formatSingleLine(line, request, response);

            // Xử lý gộp dòng trống
            if (request.getMergeEmptyLines()) {
                if (formattedLine.trim().isEmpty()) {
                    if (!previousLineEmpty) {
                        formattedLines.add(formattedLine);
                        previousLineEmpty = true;
                    }
                    // Bỏ qua dòng trống liên tiếp
                } else {
                    formattedLines.add(formattedLine);
                    previousLineEmpty = false;
                }
            } else {
                formattedLines.add(formattedLine);
            }

            response.setProcessedLines(i + 1);
            response.updateProgress();

            // Cập nhật progress mỗi 100 dòng
            if ((i + 1) % 100 == 0) {
                response.setMessage("Đã format " + (i + 1) + "/" + originalLines.size() + " dòng...");
            }
        }

        return formattedLines;
    }

    /**
     * Format một dòng đơn lẻ
     */
    private String formatSingleLine(String line, FormatFileRequest request, FormatFileResponse response) {
        String formattedLine = line;

        // 1. Loại bỏ khoảng trắng dư thừa ở đầu/cuối dòng
        formattedLine = formattedLine.trim();

        // 2. Loại bỏ watermark
        if (request.getRemoveWatermark() && WATERMARK_PATTERN.matcher(formattedLine).matches()) {
            response.getStats().setWatermarkLinesRemoved(response.getStats().getWatermarkLinesRemoved() + 1);
            return ""; // Trả về dòng trống để bị loại bỏ
        }

        // 3. Loại bỏ dòng chỉ chứa ký tự đặc biệt
        if (SPECIAL_ONLY_PATTERN.matcher(formattedLine).matches()) {
            response.getStats().setSpecialOnlyLinesRemoved(response.getStats().getSpecialOnlyLinesRemoved() + 1);
            return ""; // Trả về dòng trống để bị loại bỏ
        }

        // 4. Xử lý dòng bắt đầu bằng dấu gạch đầu dòng (loại bỏ dấu gạch, giữ lại nội
        // dung)
        if (request.getRemoveSpecialChars()) {
            Matcher dashMatcher = DASH_START_PATTERN.matcher(formattedLine);
            if (dashMatcher.matches()) {
                String content = dashMatcher.group(1).trim();
                if (!content.isEmpty()) {
                    formattedLine = content;
                    response.getStats()
                            .setSpecialCharLinesRemoved(response.getStats().getSpecialCharLinesRemoved() + 1);
                } else {
                    // Nếu chỉ có dấu gạch không có nội dung, loại bỏ hoàn toàn
                    response.getStats()
                            .setSpecialCharLinesRemoved(response.getStats().getSpecialCharLinesRemoved() + 1);
                    return ""; // Trả về dòng trống để bị loại bỏ
                }
            } else if (SPECIAL_CHAR_PATTERN.matcher(formattedLine).matches()) {
                // Xử lý các ký tự đặc biệt khác ở đầu dòng
                response.getStats().setSpecialCharLinesRemoved(response.getStats().getSpecialCharLinesRemoved() + 1);
                return ""; // Trả về dòng trống để bị loại bỏ
            }
        }

        // 5. Gộp nhiều khoảng trắng liên tiếp thành 1 khoảng trắng duy nhất
        formattedLine = formattedLine.replaceAll("\\s+", " ");

        // 6. Format dấu câu
        if (request.getFormatPunctuation()) {
            formattedLine = formatPunctuation(formattedLine, response);
        }

        return formattedLine;
    }

    /**
     * Format dấu câu: xóa khoảng trắng thừa trước dấu câu và đảm bảo có đúng 1
     * khoảng trắng sau dấu câu
     */
    private String formatPunctuation(String line, FormatFileResponse response) {
        if (line.trim().isEmpty()) {
            return line;
        }

        // Xóa khoảng trắng thừa trước dấu câu
        String formatted = line.replaceAll("\\s+([.,!?;:])", "$1");

        // Đảm bảo có đúng 1 khoảng trắng sau dấu câu (trừ khi cuối dòng)
        formatted = formatted.replaceAll("([.,!?;:])(?=\\S)", "$1 ");

        // Xóa khoảng trắng thừa ở cuối dòng
        formatted = formatted.trim();

        if (!formatted.equals(line)) {
            response.getStats().setPunctuationLinesFormatted(response.getStats().getPunctuationLinesFormatted() + 1);
        }

        return formatted;
    }

    /**
     * Lưu file đã format
     */
    private String saveFormattedFile(List<String> formattedLines, String originalFileName,
            String jobId, FormatFileResponse response) throws IOException {
        // Tạo tên file output
        String baseName = originalFileName != null ? originalFileName.replaceAll("\\.txt$", "") : "formatted";
        String formattedFileName = baseName + "_formatted_" + jobId + ".txt";

        Path filePath = Paths.get(FORMATTED_FILES_DIR, formattedFileName);

        // Lưu file với encoding UTF-8
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            for (String line : formattedLines) {
                writer.write(line);
                writer.newLine();
            }
        }

        log.info("Đã lưu file đã format: {}", filePath);
        return formattedFileName;
    }

    /**
     * Tính kích thước file
     */
    private long calculateFileSize(List<String> lines) {
        return lines.stream()
                .mapToLong(line -> line.getBytes(StandardCharsets.UTF_8).length + 1) // +1 cho newline
                .sum();
    }

    // Getter methods
    @Override
    public Map<String, FormatFileResponse> getFormatJobs() {
        return formatJobs;
    }

    @Override
    public Map<String, Long> getJobUserMap() {
        return jobUserMap;
    }

    @Override
    public Map<String, AtomicBoolean> getCancelFlags() {
        return cancelFlags;
    }
}
