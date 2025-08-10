package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.Chapter;
import com.meobeo.truyen.domain.entity.Story;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.request.story.TxtImportRequest;
import com.meobeo.truyen.domain.response.story.TxtImportResponse;
import com.meobeo.truyen.exception.BadRequestException;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.repository.ChapterRepository;
import com.meobeo.truyen.repository.StoryRepository;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.service.interfaces.TxtImportService;

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
public class TxtImportServiceImpl implements TxtImportService {

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;

    // Cache để lưu trạng thái các job đang chạy
    private final Map<String, TxtImportResponse> importJobs = new ConcurrentHashMap<>();

    // Flag để hủy job
    private final Map<String, AtomicBoolean> cancelFlags = new ConcurrentHashMap<>();

    // Pattern tìm chương trong file TXT - hỗ trợ nhiều định dạng
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
            "^(?:Chương|chương|Chapter|chapter)\\s*(\\d+)[:：]?\\s*([^\\n\\r]*)$",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    // Pattern để lọc nội dung DTV-EBOOK
    private static final Pattern DTV_EBOOK_PATTERN = Pattern.compile(
            ".*DTV-EBOOK.*", Pattern.CASE_INSENSITIVE);

    @Override
    public String startTxtImport(MultipartFile txtFile, TxtImportRequest request, Long userId) {
        // Validation
        if (txtFile == null || txtFile.isEmpty()) {
            throw new BadRequestException("File TXT không được để trống");
        }

        if (!isTxtFile(txtFile)) {
            throw new BadRequestException("File phải có định dạng TXT");
        }

        // Kiểm tra user tồn tại
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // Kiểm tra story tồn tại và quyền truy cập
        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Không tìm thấy truyện với ID: " + request.getStoryId()));

        // Kiểm tra quyền: chỉ author hoặc admin mới được import
        if (!story.getAuthor().getId().equals(userId)) {
            User user = userRepository.findById(userId).orElseThrow();
            boolean isAdmin = user.getRoles().stream()
                    .anyMatch(role -> "ADMIN".equals(role.getName()));
            if (!isAdmin) {
                throw new BadRequestException("Bạn không có quyền import chapter cho truyện này");
            }
        }

        // Tạo job ID
        String jobId = UUID.randomUUID().toString();

        // Tạo response ban đầu
        TxtImportResponse response = new TxtImportResponse();
        response.setJobId(jobId);
        response.setStatus("PROCESSING");
        response.setStartTime(LocalDateTime.now());
        response.setStoryId(story.getId());
        response.setStorySlug(story.getSlug());
        response.setStoryTitle(story.getTitle());
        response.setMessage("Đang phân tích file TXT để import chapter...");

        importJobs.put(jobId, response);
        cancelFlags.put(jobId, new AtomicBoolean(false));

        // Bắt đầu xử lý async
        processTxtFileAsync(txtFile, request, userId, jobId);

        return jobId;
    }

    @Async("taskExecutor")
    private void processTxtFileAsync(MultipartFile txtFile, TxtImportRequest request, Long userId, String jobId) {
        TxtImportResponse response = importJobs.get(jobId);
        AtomicBoolean cancelFlag = cancelFlags.get(jobId);

        try {
            // Xử lý file TXT theo stream để tối ưu memory
            response.setMessage("Đang đọc và phân tích file TXT...");

            List<ChapterData> chapters = processTxtFileByStream(txtFile, request, response, cancelFlag);

            if (cancelFlag.get()) {
                response.setStatus("CANCELLED");
                response.setMessage("Import đã bị hủy");
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
                return;
            }

            response.setTotalChapters(chapters.size());
            response.setMessage("Đã phát hiện " + chapters.size() + " chương. Bắt đầu import vào database...");

            // Import chapters theo batch
            importChaptersInBatches(request.getStoryId(), chapters, request.getBatchSize(),
                    response, request.getOverwriteExisting(), cancelFlag);

            // Hoàn thành
            response.setStatus("COMPLETED");
            response.setEndTime(LocalDateTime.now());
            response.setMessage("Import hoàn thành. Đã import " + response.getSuccessCount() + " chương thành công.");

        } catch (Exception e) {
            log.error("Lỗi import TXT: jobId={}, error={}", jobId, e.getMessage(), e);
            response.setStatus("FAILED");
            response.setEndTime(LocalDateTime.now());
            response.addError("Lỗi import: " + e.getMessage());
            response.setMessage("Import thất bại: " + e.getMessage());
        } finally {
            // Cleanup
            cancelFlags.remove(jobId);
        }
    }

    private String readTxtFile(MultipartFile file) throws Exception {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    /**
     * Đọc file TXT theo stream và xử lý từng chương theo yêu cầu
     * Tối ưu memory cho file lớn
     */
    private List<ChapterData> processTxtFileByStream(MultipartFile file, TxtImportRequest request,
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
                                currentChapterTitle = title;
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
                if (currentChapterTitle == null) {
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

    private ChapterData createChapterData(int chapterNumber, String title, String fullContent,
            TxtImportRequest request, TxtImportResponse response) {

        // Loại bỏ dòng tiêu đề chương khỏi nội dung
        String[] lines = fullContent.split("\n", 2);
        String content = (lines.length > 1) ? lines[1].trim() : "";

        ChapterData chapterData = new ChapterData();
        chapterData.number = chapterNumber;
        chapterData.title = title;
        chapterData.content = content;
        chapterData.slug = generateChapterSlug(title, chapterNumber,
                request.getChapterSlugPrefix(), response.getStorySlug());

        return chapterData;
    }

    private List<ChapterData> extractChaptersFromTxt(String fileContent, TxtImportRequest request,
            TxtImportResponse response) {
        List<ChapterData> chapters = new ArrayList<>();
        List<ChapterMatch> chapterMatches = new ArrayList<>();

        // Tìm tất cả các chương trong file
        Matcher matcher = CHAPTER_PATTERN.matcher(fileContent);
        while (matcher.find()) {
            try {
                int chapterNumber = Integer.parseInt(matcher.group(1));
                String title = matcher.group(2).trim();

                // Nếu title rỗng, tạo title mặc định
                if (title.isEmpty()) {
                    title = "Chương " + chapterNumber;
                }

                ChapterMatch match = new ChapterMatch();
                match.chapterNumber = chapterNumber;
                match.title = title;
                match.startPosition = matcher.start();
                chapterMatches.add(match);

                log.debug("Tìm thấy chương: số={}, title={}, vị trí={}",
                        chapterNumber, title, matcher.start());
            } catch (NumberFormatException e) {
                log.warn("Bỏ qua chương không hợp lệ: {}", matcher.group(0));
            }
        }

        log.info("Tổng cộng tìm thấy {} chương trong file", chapterMatches.size());

        // Sắp xếp theo vị trí trong file (không phải theo số chương)
        chapterMatches.sort(Comparator.comparingInt(m -> m.startPosition));

        // Lọc theo range yêu cầu
        List<ChapterMatch> filteredMatches = new ArrayList<>();
        for (ChapterMatch match : chapterMatches) {
            if (match.chapterNumber >= request.getStartFromChapter()) {
                if (request.getEndAtChapter() == null || match.chapterNumber <= request.getEndAtChapter()) {
                    filteredMatches.add(match);
                }
            }
        }

        // Trích xuất nội dung cho từng chương
        for (int i = 0; i < filteredMatches.size(); i++) {
            ChapterMatch currentMatch = filteredMatches.get(i);
            ChapterMatch nextMatch = (i + 1 < filteredMatches.size()) ? filteredMatches.get(i + 1) : null;

            try {
                // Xác định vị trí kết thúc của chương hiện tại
                int endPosition = (nextMatch != null) ? nextMatch.startPosition : fileContent.length();

                // Kiểm tra tính hợp lệ của vị trí
                if (currentMatch.startPosition >= endPosition) {
                    log.warn("Bỏ qua chương {} vì vị trí không hợp lệ: start={}, end={}, fileLength={}",
                            currentMatch.chapterNumber, currentMatch.startPosition, endPosition, fileContent.length());
                    continue;
                }

                // Kiểm tra giới hạn file
                if (currentMatch.startPosition >= fileContent.length()) {
                    log.warn("Bỏ qua chương {} vì vị trí vượt quá độ dài file: start={}, fileLength={}",
                            currentMatch.chapterNumber, currentMatch.startPosition, fileContent.length());
                    continue;
                }

                if (endPosition > fileContent.length()) {
                    endPosition = fileContent.length();
                }

                // Trích xuất nội dung
                String content = fileContent.substring(currentMatch.startPosition, endPosition).trim();

                // Loại bỏ dòng tiêu đề chương khỏi nội dung
                String[] lines = content.split("\n", 2);
                if (lines.length > 1) {
                    content = lines[1].trim();
                } else {
                    content = "";
                }

                // Tạo ChapterData
                ChapterData chapterData = new ChapterData();
                chapterData.number = currentMatch.chapterNumber;
                chapterData.title = currentMatch.title;
                chapterData.content = content;
                chapterData.slug = generateChapterSlug(currentMatch.title, currentMatch.chapterNumber,
                        request.getChapterSlugPrefix(), response.getStorySlug());

                chapters.add(chapterData);

                log.debug("Đã trích xuất chương {}: title={}, contentLength={}",
                        currentMatch.chapterNumber, currentMatch.title, content.length());

            } catch (Exception e) {
                log.error("Lỗi trích xuất chương {}: {}", currentMatch.chapterNumber, e.getMessage(), e);
                response.addError("Lỗi chương " + currentMatch.chapterNumber + ": " + e.getMessage());
            }
        }

        return chapters;
    }

    private void importChaptersInBatches(Long storyId, List<ChapterData> chapters, int batchSize,
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
    private void importChapterBatch(Story story, List<ChapterData> batch, TxtImportResponse response,
            Boolean overwriteExisting) {

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

                        log.info("Cập nhật chapter: storyId={}, chapterNumber={}, title={}",
                                story.getId(), chapterData.number, chapterData.title);
                    } else {
                        // Bỏ qua chapter đã tồn tại
                        log.info("Bỏ qua chapter đã tồn tại: storyId={}, chapterNumber={}",
                                story.getId(), chapterData.number);
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

                    log.info("Tạo chapter mới: storyId={}, chapterNumber={}, title={}",
                            story.getId(), chapterData.number, chapterData.title);
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
    }

    private String generateChapterSlug(String title, int chapterNumber, String chapterSlugPrefix, String storySlug) {
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

        String fullSlug = baseSlug + "-" + titleSlug;

        // Đảm bảo slug unique
        return ensureUniqueSlug(fullSlug, null);
    }

    private String ensureUniqueSlug(String originalSlug, Long excludeChapterId) {
        String slug = originalSlug;
        int counter = 1;

        while (chapterRepository.existsBySlug(slug)) {
            slug = originalSlug + "-" + counter;
            counter++;
        }

        return slug;
    }

    private boolean isTxtFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return false;
        }
        return originalFilename.toLowerCase().endsWith(".txt");
    }

    @Override
    public Optional<TxtImportResponse> getImportStatus(String jobId) {
        return Optional.ofNullable(importJobs.get(jobId));
    }

    @Override
    public boolean cancelImport(String jobId, Long userId) {
        AtomicBoolean cancelFlag = cancelFlags.get(jobId);
        if (cancelFlag != null) {
            cancelFlag.set(true);
            TxtImportResponse response = importJobs.get(jobId);
            if (response != null) {
                response.setStatus("CANCELLED");
                response.setMessage("Import đã bị hủy bởi người dùng");
            }
            return true;
        }
        return false;
    }

    @Override
    public void cleanupCompletedJob(String jobId) {
        importJobs.remove(jobId);
        cancelFlags.remove(jobId);
    }

    // Inner classes
    private static class ChapterMatch {
        int chapterNumber;
        String title;
        int startPosition;
    }

    private static class ChapterData {
        int number;
        String title;
        String slug;
        String content;
    }
}
