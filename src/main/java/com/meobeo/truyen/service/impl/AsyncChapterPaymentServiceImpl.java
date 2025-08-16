package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.Chapter;
import com.meobeo.truyen.domain.entity.ChapterPayment;
import com.meobeo.truyen.domain.entity.Story;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.request.chapter.ChapterBatchLockRequest;
import com.meobeo.truyen.domain.response.chapter.ChapterBatchLockResponse;
import com.meobeo.truyen.domain.response.chapter.ChapterPaymentResponse;
import com.meobeo.truyen.exception.BadRequestException;
import com.meobeo.truyen.exception.ForbiddenException;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.repository.ChapterPaymentRepository;
import com.meobeo.truyen.repository.ChapterRepository;
import com.meobeo.truyen.repository.StoryRepository;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.service.interfaces.AsyncChapterPaymentService;
import com.meobeo.truyen.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncChapterPaymentServiceImpl implements AsyncChapterPaymentService {

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterPaymentRepository chapterPaymentRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    // Cache để lưu trạng thái các job đang chạy
    private final Map<String, ChapterBatchLockResponse> asyncJobResults = new ConcurrentHashMap<>();

    // Map để lưu userId của từng job
    private final Map<String, Long> jobUserMap = new ConcurrentHashMap<>();

    // Flag để hủy job
    private final Map<String, AtomicBoolean> cancelFlags = new ConcurrentHashMap<>();

    @Override
    public void initializeJob(String jobId, Long userId) {
        // Tạo initial response
        ChapterBatchLockResponse initialResponse = new ChapterBatchLockResponse();
        initialResponse.setJobId(jobId);
        initialResponse.setStatus("PROCESSING");
        initialResponse.setStartTime(LocalDateTime.now());
        asyncJobResults.put(jobId, initialResponse);
        jobUserMap.put(jobId, userId);
        cancelFlags.put(jobId, new AtomicBoolean(false));
        log.info("Đã khởi tạo job tracking: jobId={}, userId={}", jobId, userId);
    }

    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processBatchLockAsyncInternal(ChapterBatchLockRequest request, Long userId, String jobId) {
        log.info("=== BẮT ĐẦU processBatchLockAsync ===");
        log.info("Thread async: {}", Thread.currentThread().getName());
        log.info("JobId: {}", jobId);

        ChapterBatchLockResponse response = asyncJobResults.get(jobId);
        AtomicBoolean cancelFlag = cancelFlags.get(jobId);

        if (response == null || cancelFlag == null) {
            log.error("Không tìm thấy job hoặc cancel flag: jobId={}", jobId);
            return;
        }

        try {
            // Cập nhật trạng thái bắt đầu xử lý
            response.setStatus("PROCESSING");
            log.info("Bắt đầu xử lý batch lock: jobId={}", jobId);

            // Xử lý async với chunks để tránh transaction quá lớn
            ChapterBatchLockResponse finalResponse = processLargeBatchInChunks(request, userId, jobId, cancelFlag);

            if (cancelFlag.get()) {
                response.setStatus("CANCELLED");
                log.info("Job bị hủy: jobId={}", jobId);
                return;
            }

            // Lưu kết quả cuối cùng
            finalResponse.setJobId(jobId);
            finalResponse.setStatus("COMPLETED");
            finalResponse.setEndTime(LocalDateTime.now());
            asyncJobResults.put(jobId, finalResponse);

            log.info("Hoàn thành async batch lock: jobId={}, success={}, failure={}",
                    jobId, finalResponse.getSuccessCount(), finalResponse.getFailureCount());

        } catch (Exception e) {
            log.error("Lỗi async batch lock: jobId={}", jobId, e);

            ChapterBatchLockResponse errorResponse = new ChapterBatchLockResponse();
            errorResponse.setJobId(jobId);
            errorResponse.setStatus("FAILED");
            errorResponse.setEndTime(LocalDateTime.now());
            errorResponse.setFailureCount(1);
            errorResponse.setSuccessCount(0);

            asyncJobResults.put(jobId, errorResponse);
        } finally {
            // Cleanup
            cancelFlags.remove(jobId);
            jobUserMap.remove(jobId);
            log.info("Hoàn thành job batch lock: jobId={}, status={}", jobId, response.getStatus());
        }
    }

    @Override
    public Optional<ChapterBatchLockResponse> getAsyncJobStatus(String jobId) {
        return Optional.ofNullable(asyncJobResults.get(jobId));
    }

    @Override
    public boolean cancelAsyncJob(String jobId, Long userId) {
        Long jobUserId = jobUserMap.get(jobId);
        if (jobUserId == null || !jobUserId.equals(userId)) {
            return false;
        }

        AtomicBoolean cancelFlag = cancelFlags.get(jobId);
        if (cancelFlag != null) {
            cancelFlag.set(true);
            log.info("Job đã được đánh dấu hủy: jobId={}", jobId);
            return true;
        }
        return false;
    }

    /**
     * Xử lý batch lớn bằng cách chia thành chunks nhỏ
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private ChapterBatchLockResponse processLargeBatchInChunks(ChapterBatchLockRequest request, Long userId,
            String jobId, AtomicBoolean cancelFlag) {
        final int CHUNK_SIZE = 50; // Xử lý 50 chapter mỗi lần

        // Tạo initial response để lưu progress
        ChapterBatchLockResponse initialResponse = new ChapterBatchLockResponse();
        initialResponse.setJobId(jobId);
        initialResponse.setStatus("PROCESSING");
        initialResponse.setStartTime(LocalDateTime.now());
        asyncJobResults.put(jobId, initialResponse);

        int totalSuccessCount = 0;
        int totalFailureCount = 0;
        int totalSkippedCount = 0;
        Set<String> allErrorMessages = new HashSet<>();

        int start = request.getChapterStart();
        int end = request.getChapterEnd();
        int totalChapters = end - start + 1;

        // Lấy story info
        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Không tìm thấy story với ID: " + request.getStoryId()));

        // Xử lý từng chunk
        for (int chunkStart = start; chunkStart <= end; chunkStart += CHUNK_SIZE) {
            if (cancelFlag.get()) {
                break;
            }

            int chunkEnd = Math.min(chunkStart + CHUNK_SIZE - 1, end);

            try {
                // Tạo request cho chunk này
                ChapterBatchLockRequest chunkRequest = new ChapterBatchLockRequest();
                chunkRequest.setStoryId(request.getStoryId());
                chunkRequest.setChapterStart(chunkStart);
                chunkRequest.setChapterEnd(chunkEnd);
                chunkRequest.setPrice(request.getPrice());
                chunkRequest.setIsVipOnly(request.getIsVipOnly());

                // Xử lý chunk với transaction riêng
                ChapterBatchLockResponse chunkResponse = processChunkSeparately(chunkRequest, userId);

                // Tổng hợp kết quả
                totalSuccessCount += chunkResponse.getSuccessCount();
                totalFailureCount += chunkResponse.getFailureCount();
                totalSkippedCount += chunkResponse.getSkippedCount();

                if (chunkResponse.getErrorMessages() != null) {
                    allErrorMessages.addAll(chunkResponse.getErrorMessages());
                }

                int processed = chunkEnd - start + 1;
                log.info("Hoàn thành chunk {}-{}: jobId={}, progress={}/{}",
                        chunkStart, chunkEnd, jobId, processed, totalChapters);

                // Update progress trong map
                ChapterBatchLockResponse progressResponse = new ChapterBatchLockResponse();
                progressResponse.setJobId(jobId);
                progressResponse.setStatus("PROCESSING");
                progressResponse.setStartTime(initialResponse.getStartTime());
                progressResponse.setStoryId(story.getId());
                progressResponse.setStoryTitle(story.getTitle());
                progressResponse.setStorySlug(story.getSlug());
                progressResponse.setTotalChaptersRequested(totalChapters);
                progressResponse.setTotalChaptersProcessed(processed);
                progressResponse.setSuccessCount(totalSuccessCount);
                progressResponse.setFailureCount(totalFailureCount);
                progressResponse.setSkippedCount(totalSkippedCount);
                progressResponse.calculateProgress();

                if (!allErrorMessages.isEmpty()) {
                    progressResponse.setErrorMessages(new ArrayList<>(allErrorMessages));
                }

                asyncJobResults.put(jobId, progressResponse);

            } catch (Exception e) {
                log.error("Lỗi xử lý chunk {}-{}: jobId={}", chunkStart, chunkEnd, jobId, e);

                // Thêm failure cho toàn bộ chunk
                int chunkSize = chunkEnd - chunkStart + 1;
                totalFailureCount += chunkSize;
                allErrorMessages.add("Lỗi chunk: " + e.getMessage());
            }
        }

        // Tạo response cuối cùng
        ChapterBatchLockResponse finalResponse = new ChapterBatchLockResponse();
        finalResponse.setJobId(jobId);
        finalResponse.setStoryId(story.getId());
        finalResponse.setStoryTitle(story.getTitle());
        finalResponse.setStorySlug(story.getSlug());
        finalResponse.setTotalChaptersRequested(totalChapters);
        finalResponse.setTotalChaptersProcessed(totalChapters);
        finalResponse.setSuccessCount(totalSuccessCount);
        finalResponse.setFailureCount(totalFailureCount);
        finalResponse.setSkippedCount(totalSkippedCount);
        finalResponse.calculateProgress();
        finalResponse.setStartTime(initialResponse.getStartTime());

        if (!allErrorMessages.isEmpty()) {
            finalResponse.setErrorMessages(new ArrayList<>(allErrorMessages));
            // Lấy lỗi chính
            String mainError = allErrorMessages.stream()
                    .max(Comparator.comparing(error -> allErrorMessages.stream().filter(e -> e.equals(error)).count()))
                    .orElse("Không thể tạo payment setting");
            finalResponse.setMainError(mainError);
        }

        return finalResponse;
    }

    /**
     * Xử lý một chunk với transaction riêng biệt
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private ChapterBatchLockResponse processChunkSeparately(ChapterBatchLockRequest request, Long userId) {
        log.info("Xử lý chunk batch: storyId={}, range={}-{}, userId={}",
                request.getStoryId(), request.getChapterStart(), request.getChapterEnd(), userId);

        // Kiểm tra quyền quản lý story
        if (!canManageStory(request.getStoryId(), userId)) {
            throw new ForbiddenException("Bạn không có quyền khóa chapter của story này");
        }

        // Lấy thông tin story
        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Không tìm thấy story với ID: " + request.getStoryId()));

        // Lấy chapters trong range
        List<Chapter> chaptersToLock = getChaptersByRange(request.getStoryId(), request.getChapterStart(),
                request.getChapterEnd());

        if (chaptersToLock.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy chapter nào trong khoảng " +
                    request.getChapterStart() + " - " + request.getChapterEnd());
        }

        // Check batch tất cả chapter đã khóa trước
        List<Long> chapterIds = chaptersToLock.stream().map(Chapter::getId).collect(Collectors.toList());
        Set<Long> lockedChapterIds = chapterPaymentRepository.findLockedChapterIds(chapterIds)
                .stream().collect(Collectors.toSet());

        List<ChapterPaymentResponse> successfulLocks = new ArrayList<>();
        List<String> failureReasons = new ArrayList<>();

        // Xử lý từng chapter với retry logic để tránh transaction conflict
        for (Chapter chapter : chaptersToLock) {
            try {
                // Bỏ qua chapter đã khóa
                if (lockedChapterIds.contains(chapter.getId())) {
                    log.debug("Bỏ qua chapter đã khóa: chapterId={}, chapterNumber={}",
                            chapter.getId(), chapter.getChapterNumber());
                    continue;
                }

                // Tạo record khóa chapter với retry logic
                int insertedRows = insertChapterPaymentWithRetry(
                        chapter.getId(),
                        story.getId(),
                        request.getPrice(),
                        request.getIsVipOnly() != null ? request.getIsVipOnly() : false);

                if (insertedRows > 0) {
                    // Tạo response trực tiếp thay vì lấy lại entity để tránh transaction conflict
                    ChapterPaymentResponse response = new ChapterPaymentResponse();
                    response.setChapterId(chapter.getId());
                    response.setStoryId(story.getId());
                    response.setPrice(request.getPrice());
                    response.setIsVipOnly(request.getIsVipOnly() != null ? request.getIsVipOnly() : false);
                    response.setIsLocked(true);
                    response.setChapterNumber(chapter.getChapterNumber());
                    response.setChapterTitle(chapter.getTitle());
                    response.setChapterSlug(chapter.getSlug());

                    successfulLocks.add(response);
                    log.info("Đã khóa chapter thành công: chapterId={}, chapterNumber={}",
                            chapter.getId(), chapter.getChapterNumber());
                } else {
                    failureReasons.add("Không thể tạo payment setting");
                }

            } catch (Exception e) {
                log.error("Lỗi khi khóa chapter {}: {}", chapter.getId(), e.getMessage());
                failureReasons.add("Lỗi hệ thống: " + e.getMessage());
            }
        }

        // Tạo response với format mới
        ChapterBatchLockResponse response = new ChapterBatchLockResponse();
        response.setStoryId(story.getId());
        response.setStoryTitle(story.getTitle());
        response.setStorySlug(story.getSlug());

        // Thống kê tổng quan
        response.setTotalChaptersRequested(chaptersToLock.size());
        response.setTotalChaptersProcessed(chaptersToLock.size());
        response.setSuccessCount(successfulLocks.size());
        response.setFailureCount(failureReasons.size());
        response.setSkippedCount(0); // Sẽ được tính trong processLargeBatchInChunks

        // Tính tiến độ
        response.calculateProgress();

        // Thu thập lỗi chính xác (không trùng lặp)
        if (!failureReasons.isEmpty()) {
            Set<String> uniqueErrors = new HashSet<>(failureReasons);

            for (String error : uniqueErrors) {
                response.addErrorMessage(error);
            }

            // Lấy lỗi chính (lỗi xuất hiện nhiều nhất)
            String mainError = uniqueErrors.stream()
                    .max(Comparator.comparing(error -> failureReasons.stream().filter(e -> e.equals(error)).count()))
                    .orElse("Không thể tạo payment setting");
            response.setMainError(mainError);
        }

        log.info("Hoàn thành chunk batch: total={}, success={}, failure={}",
                chaptersToLock.size(), successfulLocks.size(), failureReasons.size());

        return response;
    }

    /**
     * Kiểm tra quyền quản lý story (sử dụng userId thay vì SecurityContext)
     */
    private boolean canManageStory(Long storyId, Long userId) {
        // Lấy thông tin user từ database
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Không tìm thấy user với ID: {}", userId);
            return false;
        }

        // Kiểm tra role của user
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getName()));

        boolean isUploader = user.getRoles().stream()
                .anyMatch(role -> "UPLOADER".equals(role.getName()));

        log.debug("Kiểm tra quyền: userId={}, isAdmin={}, isUploader={}", userId, isAdmin, isUploader);

        // Admin có thể manage tất cả stories
        if (isAdmin) {
            return true;
        }

        // Uploader chỉ có thể manage story mình tạo
        if (isUploader) {
            return storyRepository.findById(storyId)
                    .map(story -> {
                        log.debug("Kiểm tra quyền story: authorId={}, userId={}", story.getAuthor().getId(), userId);
                        return story.getAuthor().getId().equals(userId);
                    })
                    .orElse(false);
        }

        // USER role hoặc role khác không có quyền
        return false;
    }

    /**
     * Lấy danh sách chapter theo range chapter number
     */
    private List<Chapter> getChaptersByRange(Long storyId, Integer chapterStart, Integer chapterEnd) {
        return chapterRepository.findByStoryIdAndChapterNumberBetweenOrderByChapterNumber(
                storyId, chapterStart, chapterEnd);
    }

    /**
     * Mapping từ Entity sang Response DTO
     */
    private ChapterPaymentResponse mapToResponse(ChapterPayment payment) {
        ChapterPaymentResponse response = new ChapterPaymentResponse();
        response.setChapterId(payment.getChapterId());
        response.setPrice(payment.getPrice());
        response.setIsVipOnly(payment.getIsVipOnly());
        response.setIsLocked(payment.getIsLocked());
        response.setStoryId(payment.getStoryId());

        // Thông tin chapter
        if (payment.getChapter() != null) {
            response.setChapterNumber(payment.getChapter().getChapterNumber());
            response.setChapterTitle(payment.getChapter().getTitle());
            response.setChapterSlug(payment.getChapter().getSlug());
        }

        return response;
    }

    /**
     * Tạo ChapterPayment entity an toàn để tránh transaction conflict
     * Sử dụng native SQL INSERT với ON CONFLICT DO NOTHING để tránh duplicate key
     * exception
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private int insertChapterPaymentSafely(Long chapterId, Long storyId, Integer price, Boolean isVipOnly) {
        try {
            // Sử dụng native SQL INSERT với ON CONFLICT DO NOTHING
            // Điều này sẽ tránh duplicate key exception và transaction conflict
            int insertedRows = chapterPaymentRepository.insertChapterPaymentIgnoreDuplicate(
                    chapterId,
                    storyId,
                    price,
                    isVipOnly != null ? isVipOnly : false,
                    true // isLocked = true
            );

            if (insertedRows > 0) {
                log.debug("Đã tạo ChapterPayment thành công: chapterId={}", chapterId);
                return 1;
            } else {
                log.debug("ChapterPayment đã tồn tại hoặc không thể insert: chapterId={}", chapterId);
                return 0;
            }

        } catch (Exception e) {
            log.error("Lỗi khi tạo ChapterPayment: chapterId={}, error={}", chapterId, e.getMessage());
            return 0;
        }
    }

    /**
     * Tạo ChapterPayment với retry logic để xử lý transaction conflict
     */
    private int insertChapterPaymentWithRetry(Long chapterId, Long storyId, Integer price, Boolean isVipOnly) {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                return insertChapterPaymentSafely(chapterId, storyId, price, isVipOnly);
            } catch (Exception e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    log.error("Đã thử {} lần nhưng vẫn lỗi khi tạo ChapterPayment: chapterId={}, error={}",
                            maxRetries, chapterId, e.getMessage());
                    return 0;
                }

                // Chờ một chút trước khi thử lại
                try {
                    Thread.sleep(100 * retryCount); // Tăng thời gian chờ theo số lần retry
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return 0;
                }

                log.debug("Retry lần {} cho chapterId={}", retryCount, chapterId);
            }
        }

        return 0;
    }
}
