package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.Chapter;
import com.meobeo.truyen.domain.entity.Story;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.request.story.TxtImportRequest;
import com.meobeo.truyen.domain.response.story.TxtImportResponse;
import com.meobeo.truyen.repository.ChapterRepository;
import com.meobeo.truyen.repository.StoryRepository;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.service.interfaces.AsyncTxtImportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncTxtImportServiceImpl implements AsyncTxtImportService {

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;

    // Cache để lưu trạng thái các job đang chạy
    private final Map<String, TxtImportResponse> importJobs = new ConcurrentHashMap<>();

    // Map để lưu userId của từng job
    private final Map<String, Long> jobUserMap = new ConcurrentHashMap<>();

    // Flag để hủy job
    private final Map<String, AtomicBoolean> cancelFlags = new ConcurrentHashMap<>();

    // Pattern tìm chương trong file TXT - hỗ trợ nhiều định dạng
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
            "^(?:Chương|chương|Chapter|chapter)\\s*(\\d+)[:：]?\\s*([^\\n\\r]*)$",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    // Pattern để lọc nội dung DTV-EBOOK
    private static final Pattern DTV_EBOOK_PATTERN = Pattern.compile(
            ".*DTV-EBOOK.*", Pattern.CASE_INSENSITIVE);

    @Async("txtImportExecutor")
    @Override
    public void processTxtFileAsync(MultipartFile txtFile, TxtImportRequest request, Long userId, String jobId) {
        log.info("=== BẮT ĐẦU processTxtFileAsync ===");
        log.info("Thread async: {}", Thread.currentThread().getName());
        log.info("JobId: {}", jobId);

        TxtImportResponse response = importJobs.get(jobId);
        AtomicBoolean cancelFlag = cancelFlags.get(jobId);

        if (response == null || cancelFlag == null) {
            log.error("Không tìm thấy job hoặc cancel flag: jobId={}", jobId);
            return;
        }

        try {
            // Cập nhật trạng thái bắt đầu xử lý
            response.setStatus("PROCESSING");
            response.setMessage("Đang đọc và phân tích file TXT...");
            log.info("Bắt đầu xử lý file TXT: jobId={}", jobId);

            // Xử lý file TXT theo stream để tối ưu memory
            List<ChapterData> chapters = processTxtFileByStream(txtFile, request, response, cancelFlag);

            if (cancelFlag.get()) {
                response.setStatus("CANCELLED");
                response.setMessage("Import đã bị hủy");
                log.info("Job bị hủy: jobId={}", jobId);
                return;
            }

            if (chapters.isEmpty()) {
                response.setStatus("FAILED");
                response.setMessage(
                        "Không tìm thấy chương nào trong range yêu cầu. Vui lòng kiểm tra định dạng file và range chương.");
                response.addError(
                        "Không tìm thấy chương nào phù hợp với pattern: Chương X hoặc Chapter X trong range " +
                                request.getStartFromChapter() + " - "
                                + (request.getEndAtChapter() != null ? request.getEndAtChapter() : "cuối"));
                log.warn("Không tìm thấy chương nào: jobId={}", jobId);
                return;
            }

            response.setTotalChapters(chapters.size());
            response.setMessage("Đã phát hiện " + chapters.size() + " chương. Bắt đầu import vào database...");
            log.info("Đã phát hiện {} chương, bắt đầu import: jobId={}", chapters.size(), jobId);

            // Import chapters theo batch
            importChaptersInBatches(request.getStoryId(), chapters, request.getBatchSize(),
                    response, request.getOverwriteExisting(), cancelFlag);

            if (cancelFlag.get()) {
                response.setStatus("CANCELLED");
                response.setMessage("Import đã bị hủy");
                log.info("Job bị hủy trong quá trình import: jobId={}", jobId);
                return;
            }

            // Hoàn thành
            response.setStatus("COMPLETED");
            response.setEndTime(LocalDateTime.now());
            response.setMessage("Import hoàn thành. Đã import " + response.getSuccessCount() + " chương thành công.");
            log.info("Import hoàn thành: jobId={}, successCount={}, failureCount={}",
                    jobId, response.getSuccessCount(), response.getFailureCount());

        } catch (Exception e) {
            log.error("Lỗi import TXT: jobId={}, error={}", jobId, e.getMessage(), e);
            response.setStatus("FAILED");
            response.setEndTime(LocalDateTime.now());
            response.addError("Lỗi import: " + e.getMessage());
            response.setMessage("Import thất bại: " + e.getMessage());
        } finally {
            // Cleanup
            cancelFlags.remove(jobId);
            jobUserMap.remove(jobId);
            log.info("Hoàn thành job import: jobId={}, status={}", jobId, response.getStatus());
        }
    }

    /**
     * Đọc file TXT theo stream và xử lý từng chương theo yêu cầu
     * Tối ưu memory cho file lớn
     */
    public List<ChapterData> processTxtFileByStream(MultipartFile file, TxtImportRequest request,
            TxtImportResponse response, AtomicBoolean cancelFlag) throws Exception {

        List<ChapterData> chapters = new ArrayList<>();
        StringBuilder currentChapterContent = new StringBuilder();
        String currentChapterTitle = null;
        Integer currentChapterNumber = null;
        boolean isInTargetRange = false;
        boolean waitingForTitle = false; // Flag để đợi title ở dòng tiếp theo
        int processedChapters = 0;
        int totalChaptersFound = 0;

        log.info("Bắt đầu xử lý file TXT theo stream, range: {} - {}",
                request.getStartFromChapter(),
                request.getEndAtChapter() != null ? request.getEndAtChapter() : "cuối");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (cancelFlag.get()) {
                    break;
                }

                // Loại bỏ dòng có chứa DTV-EBOOK
                if (DTV_EBOOK_PATTERN.matcher(line).matches()) {
                    log.debug("Bỏ qua dòng có DTV-EBOOK: {}", line);
                    continue;
                }

                // Kiểm tra xem có phải là dòng bắt đầu chương mới không
                Matcher chapterMatcher = CHAPTER_PATTERN.matcher(line);
                if (chapterMatcher.find()) {
                    totalChaptersFound++;

                    // Lưu chương trước đó nếu đang trong target range
                    if (isInTargetRange && currentChapterNumber != null) {
                        ChapterData chapterData = createChapterData(currentChapterNumber, currentChapterTitle,
                                currentChapterContent.toString(), request, response);
                        chapters.add(chapterData);
                        processedChapters++;

                        log.debug("Đã xử lý chương {}: {}", currentChapterNumber, currentChapterTitle);

                        // Cập nhật progress
                        response.setProcessedCount(processedChapters);
                        response.setMessage("Đã xử lý " + processedChapters + " chương...");
                    }

                    // Xử lý chương mới
                    try {
                        int chapterNumber = Integer.parseInt(chapterMatcher.group(1));
                        String title = chapterMatcher.group(2).trim();

                        // Kiểm tra xem có trong range yêu cầu không
                        boolean inStartRange = chapterNumber >= request.getStartFromChapter();
                        boolean inEndRange = request.getEndAtChapter() == null
                                || chapterNumber <= request.getEndAtChapter();

                        if (inStartRange && inEndRange) {
                            isInTargetRange = true;
                            currentChapterNumber = chapterNumber;
                            currentChapterContent = new StringBuilder();
                            currentChapterContent.append(line).append("\n");

                            // Xử lý title
                            if (title.isEmpty()) {
                                // Title rỗng, đợi dòng tiếp theo
                                waitingForTitle = true;
                                currentChapterTitle = null;
                            } else {
                                // Có title ngay trên dòng này
                                waitingForTitle = false;
                                currentChapterTitle = title.trim();
                            }

                            log.debug("Bắt đầu xử lý chương {}: {}", chapterNumber,
                                    currentChapterTitle != null ? currentChapterTitle : "(đợi title)");
                        } else {
                            isInTargetRange = false;
                            currentChapterNumber = null;
                            currentChapterTitle = null;
                            currentChapterContent = new StringBuilder();
                            waitingForTitle = false;
                        }

                    } catch (NumberFormatException e) {
                        log.warn("Bỏ qua dòng không hợp lệ: {}", line);
                        isInTargetRange = false;
                        waitingForTitle = false;
                    }
                } else {
                    // Thêm nội dung vào chương hiện tại nếu đang trong target range
                    if (isInTargetRange && currentChapterNumber != null) {
                        // Kiểm tra xem có phải đang đợi title không
                        if (waitingForTitle && currentChapterTitle == null) {
                            // Dòng này có thể là title của chương
                            String potentialTitle = line.trim();
                            if (!potentialTitle.isEmpty() && !potentialTitle.matches("^\\s*$")) {
                                currentChapterTitle = potentialTitle;
                                waitingForTitle = false;
                                log.debug("Lấy title từ dòng tiếp theo cho chương {}: {}",
                                        currentChapterNumber, currentChapterTitle);
                            }
                        }

                        currentChapterContent.append(line).append("\n");

                        // Giới hạn độ dài content để tránh memory overflow
                        if (currentChapterContent.length() > 1000000) { // 1MB limit
                            log.warn("Chương {} quá dài, cắt bớt nội dung", currentChapterNumber);
                            break;
                        }
                    }
                }
            }

            // Xử lý chương cuối cùng
            if (isInTargetRange && currentChapterNumber != null) {
                // Nếu vẫn chưa có title, tạo title mặc định
                if (currentChapterTitle == null || currentChapterTitle.trim().isEmpty()) {
                    currentChapterTitle = "Chương " + currentChapterNumber;
                }

                ChapterData chapterData = createChapterData(currentChapterNumber, currentChapterTitle,
                        currentChapterContent.toString(), request, response);
                chapters.add(chapterData);
                processedChapters++;

                log.debug("Đã xử lý chương cuối {}: {}", currentChapterNumber, currentChapterTitle);
            }
        }

        log.info("Hoàn thành xử lý stream, tổng cộng {} chương trong file, {} chương trong range yêu cầu",
                totalChaptersFound, chapters.size());
        return chapters;
    }

    public ChapterData createChapterData(int chapterNumber, String title, String fullContent,
            TxtImportRequest request, TxtImportResponse response) {

        // Loại bỏ dòng tiêu đề chương khỏi nội dung
        String[] lines = fullContent.split("\n", 2);
        String content = (lines.length > 1) ? lines[1].trim() : "";

        // Đảm bảo title không null
        String safeTitle = (title != null && !title.trim().isEmpty()) ? title.trim() : "Chương " + chapterNumber;
        ChapterData chapterData = new ChapterData();
        chapterData.number = chapterNumber;
        chapterData.title = safeTitle;
        chapterData.content = content;
        chapterData.slug = generateChapterSlug(safeTitle, chapterNumber,
                request.getChapterSlugPrefix(), response.getStorySlug());

        return chapterData;
    }

    public void importChaptersInBatches(Long storyId, List<ChapterData> chapters, int batchSize,
            TxtImportResponse response, Boolean overwriteExisting, AtomicBoolean cancelFlag) {

        Story story = storyRepository.findById(storyId).orElseThrow();
        int totalBatches = (int) Math.ceil((double) chapters.size() / batchSize);
        response.setTotalBatches(totalBatches);

        // Reset processed count vì đã được set trong stream processing
        response.setProcessedCount(0);

        for (int i = 0; i < chapters.size(); i += batchSize) {
            if (cancelFlag.get()) {
                response.setStatus("CANCELLED");
                response.setMessage("Import đã bị hủy");
                return;
            }

            int endIndex = Math.min(i + batchSize, chapters.size());
            List<ChapterData> batch = chapters.subList(i, endIndex);

            response.setCurrentBatch((i / batchSize) + 1);
            response.setMessage("Đang import batch " + response.getCurrentBatch() + "/" + totalBatches +
                    " (" + response.getSuccessCount() + "/" + chapters.size() + " chương đã import)");

            try {
                importChapterBatch(story, batch, response, overwriteExisting);
            } catch (Exception e) {
                log.error("Lỗi import batch: {}", e.getMessage(), e);
                response.addError("Lỗi batch " + response.getCurrentBatch() + ": " + e.getMessage());
            }
        }
    }

    @Transactional
    public void importChapterBatch(Story story, List<ChapterData> batch, TxtImportResponse response,
            Boolean overwriteExisting) {

        // Thêm timeout cho transaction
        try {
            for (ChapterData chapterData : batch) {
                try {
                    // Kiểm tra chapter đã tồn tại chưa
                    Optional<Chapter> existingChapter = chapterRepository.findByStoryIdAndChapterNumber(
                            story.getId(), chapterData.number);

                    if (existingChapter.isPresent()) {
                        if (overwriteExisting) {
                            // Cập nhật chapter hiện tại
                            Chapter chapter = existingChapter.get();
                            chapter.setTitle(chapterData.title);
                            chapter.setContent(chapterData.content);
                            chapter.setSlug(chapterData.slug);
                            chapterRepository.save(chapter);
                        } else {
                            // Bỏ qua chapter đã tồn tại
                            response.setProcessedCount(response.getProcessedCount() + 1);
                            continue;
                        }
                    } else {
                        // Tạo chapter mới
                        Chapter chapter = new Chapter();
                        chapter.setStory(story);
                        chapter.setChapterNumber(chapterData.number);
                        chapter.setTitle(chapterData.title);
                        chapter.setContent(chapterData.content);
                        chapter.setSlug(chapterData.slug);
                        chapterRepository.save(chapter);
                    }

                    response.setSuccessCount(response.getSuccessCount() + 1);
                    response.setProcessedCount(response.getProcessedCount() + 1);
                    response.updateProgress();

                } catch (Exception e) {
                    log.error("Lỗi import chapter: storyId={}, chapterNumber={}, error={}",
                            story.getId(), chapterData.number, e.getMessage(), e);
                    response.setFailureCount(response.getFailureCount() + 1);
                    response.setProcessedCount(response.getProcessedCount() + 1);
                    response.addError("Lỗi chapter " + chapterData.number + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Lỗi transaction trong batch: {}", e.getMessage(), e);
            throw e;
        }
    }

    public String generateChapterSlug(String title, int chapterNumber, String chapterSlugPrefix, String storySlug) {

        // Đảm bảo title không null
        if (title == null || title.trim().isEmpty()) {
            title = "Chương " + chapterNumber;
        }
        String baseSlug = StringUtils.hasText(chapterSlugPrefix) ? chapterSlugPrefix : storySlug;

        // Tạo slug từ title
        String titleSlug = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();

        if (titleSlug.isEmpty()) {
            titleSlug = "chuong-" + chapterNumber;
        }

        // Tối ưu: thêm timestamp để tránh conflict, không cần query database
        return baseSlug + "-" + titleSlug + "-" + System.currentTimeMillis();
    }

    // Inner classes
    private static class ChapterData {
        int number;
        String title;
        String slug;
        String content;
    }

    // Getter methods để TxtImportServiceImpl có thể truy cập
    public Map<String, TxtImportResponse> getImportJobs() {
        return importJobs;
    }

    public Map<String, Long> getJobUserMap() {
        return jobUserMap;
    }

    public Map<String, AtomicBoolean> getCancelFlags() {
        return cancelFlags;
    }
}
