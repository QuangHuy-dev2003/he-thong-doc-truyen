package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.Chapter;
import com.meobeo.truyen.domain.entity.ChapterPayment;
import com.meobeo.truyen.domain.entity.Story;
import com.meobeo.truyen.domain.request.chapter.ChapterBatchLockRequest;
import com.meobeo.truyen.domain.request.chapter.ChapterLockRequest;
import com.meobeo.truyen.domain.response.chapter.ChapterBatchLockResponse;
import com.meobeo.truyen.domain.response.chapter.ChapterPaymentResponse;

import com.meobeo.truyen.exception.BadRequestException;
import com.meobeo.truyen.exception.ForbiddenException;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.repository.ChapterPaymentRepository;
import com.meobeo.truyen.repository.ChapterRepository;
import com.meobeo.truyen.repository.StoryRepository;
import com.meobeo.truyen.service.interfaces.ChapterPaymentService;
import com.meobeo.truyen.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterPaymentServiceImpl implements ChapterPaymentService {

    private final ChapterPaymentRepository chapterPaymentRepository;
    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;
    private final SecurityUtils securityUtils;

    // Store async job results in memory (trong production nên dùng Redis)
    private final ConcurrentHashMap<String, ChapterBatchLockResponse> asyncJobResults = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public ChapterPaymentResponse lockChapter(Long chapterId, ChapterLockRequest request, Long userId) {
        log.info("Khóa chapter: chapterId={}, userId={}, price={}", chapterId, userId, request.getPrice());

        // Kiểm tra quyền
        if (!canManageChapterPayment(chapterId, userId)) {
            throw new ForbiddenException("Bạn không có quyền khóa chapter này");
        }

        // Lấy chapter với thông tin story
        Chapter chapter = chapterRepository.findByIdWithStory(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chapter với ID: " + chapterId));

        // Kiểm tra chapter đã bị khóa chưa
        boolean exists = chapterPaymentRepository.existsByChapterId(chapterId);
        log.info("CHECK: existsByChapterId({}) = {}", chapterId, exists);

        if (exists) {
            log.error("Chapter {} đã bị khóa rồi!", chapterId);
            throw new BadRequestException("Chapter đã bị khóa rồi");
        }

        log.info("Chapter {} chưa bị khóa, tiến hành tạo record mới", chapterId);

        // Tạo record khóa chapter mới bằng native query để tránh @MapsId conflict
        int insertedRows = chapterPaymentRepository.insertChapterPayment(
                chapterId,
                chapter.getStory().getId(),
                request.getPrice(),
                request.getIsVipOnly() != null ? request.getIsVipOnly() : false,
                true // isLocked = true
        );

        if (insertedRows == 0) {
            throw new RuntimeException("Không thể tạo payment setting cho chapter: " + chapterId);
        }

        // Lấy lại entity sau khi insert để trả về response
        ChapterPayment savedPayment = chapterPaymentRepository.findByChapterId(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy payment setting vừa tạo"));

        log.info("Đã khóa chapter thành công: chapterId={}, price={}", chapterId, request.getPrice());

        return mapToResponse(savedPayment);
    }

    @Override
    @Transactional
    public ChapterPaymentResponse unlockChapter(Long chapterId, Long userId) {
        log.info("Mở khóa chapter: chapterId={}, userId={}", chapterId, userId);

        // Kiểm tra quyền
        if (!canManageChapterPayment(chapterId, userId)) {
            throw new ForbiddenException("Bạn không có quyền mở khóa chapter này");
        }

        // Lấy payment setting
        ChapterPayment chapterPayment = chapterPaymentRepository.findByChapterIdWithDetails(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thông tin payment cho chapter: " + chapterId));

        chapterPayment.setIsLocked(false);
        ChapterPayment savedPayment = chapterPaymentRepository.save(chapterPayment);

        log.info("Đã mở khóa chapter thành công: chapterId={}", chapterId);

        return mapToResponse(savedPayment);
    }

    @Override
    @Transactional
    public ChapterPaymentResponse updateChapterPayment(Long chapterId, ChapterLockRequest request, Long userId) {
        log.info("Cập nhật payment chapter: chapterId={}, userId={}", chapterId, userId);

        // Kiểm tra quyền
        if (!canManageChapterPayment(chapterId, userId)) {
            throw new ForbiddenException("Bạn không có quyền cập nhật payment cho chapter này");
        }

        // Lấy payment setting
        ChapterPayment chapterPayment = chapterPaymentRepository.findByChapterIdWithDetails(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thông tin payment cho chapter: " + chapterId));

        chapterPayment.setPrice(request.getPrice());
        chapterPayment
                .setIsVipOnly(request.getIsVipOnly() != null ? request.getIsVipOnly() : chapterPayment.getIsVipOnly());
        chapterPayment
                .setIsLocked(request.getIsLocked() != null ? request.getIsLocked() : chapterPayment.getIsLocked());

        ChapterPayment savedPayment = chapterPaymentRepository.save(chapterPayment);

        log.info("Đã cập nhật payment chapter thành công: chapterId={}", chapterId);

        return mapToResponse(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public ChapterPaymentResponse getChapterPaymentInfo(Long chapterId) {
        ChapterPayment chapterPayment = chapterPaymentRepository.findByChapterIdWithDetails(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thông tin payment cho chapter: " + chapterId));

        return mapToResponse(chapterPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChapterPaymentResponse> getChapterPaymentsByStory(Long storyId, Long userId) {
        log.info("Lấy danh sách payment của story: storyId={}, userId={}", storyId, userId);

        // Kiểm tra quyền trước khi cho phép xem danh sách payments
        if (!canManageStory(storyId, userId)) {
            throw new ForbiddenException("Bạn không có quyền xem thông tin payment của story này");
        }

        List<ChapterPayment> payments = chapterPaymentRepository.findByStoryIdOrderByChapterNumber(storyId);
        return payments.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isChapterLocked(Long chapterId) {
        return chapterPaymentRepository.isChapterLocked(chapterId).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canManageChapterPayment(Long chapterId, Long userId) {
        // Admin có thể manage tất cả
        if (securityUtils.isAdmin()) {
            return true;
        }

        // Uploader chỉ có thể manage chapter của story mình tạo
        if (securityUtils.isUploader()) {
            Chapter chapter = chapterRepository.findByIdWithStory(chapterId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chapter với ID: " + chapterId));

            // Kiểm tra chặt chẽ: chỉ author của story mới có quyền
            return chapter.getStory().getAuthor().getId().equals(userId);
        }

        // USER role hoặc role khác không có quyền
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canManageStory(Long storyId, Long userId) {
        // Admin có thể manage tất cả stories
        if (securityUtils.isAdmin()) {
            return true;
        }

        // Uploader chỉ có thể manage story mình tạo
        if (securityUtils.isUploader()) {
            return storyRepository.findById(storyId)
                    .map(story -> {
                        // Kiểm tra chặt chẽ: chỉ author của story mới có quyền
                        log.debug("Kiểm tra quyền story: authorId={}, userId={}", story.getAuthor().getId(), userId);
                        return story.getAuthor().getId().equals(userId);
                    })
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy story với ID: " + storyId));
        }

        // USER role hoặc role khác không có quyền
        return false;
    }

    @Override
    @Transactional
    public void removeChapterPayment(Long chapterId, Long userId) {
        log.info("Xóa payment setting: chapterId={}, userId={}", chapterId, userId);

        // Kiểm tra quyền
        if (!canManageChapterPayment(chapterId, userId)) {
            throw new ForbiddenException("Bạn không có quyền xóa payment setting cho chapter này");
        }

        // Xóa payment setting nếu tồn tại
        chapterPaymentRepository.findByChapterId(chapterId).ifPresent(payment -> {
            chapterPaymentRepository.delete(payment);
            log.info("Đã xóa payment setting cho chapter: {}", chapterId);
        });
    }

    @Override
    @Transactional
    public ChapterBatchLockResponse lockChaptersBatch(ChapterBatchLockRequest request, Long userId) {
        log.info("Khóa batch chapter: storyId={}, userId={}", request.getStoryId(), userId);

        // Validation request
        if (!request.isValid()) {
            throw new BadRequestException("Request không hợp lệ. Phải có chapterId hoặc (chapterStart + chapterEnd)");
        }

        // Kiểm tra quyền quản lý story
        if (!canManageStory(request.getStoryId(), userId)) {
            throw new ForbiddenException("Bạn không có quyền khóa chapter của story này");
        }

        // Lấy thông tin story
        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Không tìm thấy story với ID: " + request.getStoryId()));

        List<Chapter> chaptersToLock = new ArrayList<>();

        if (request.isSingleChapter()) {
            // Khóa 1 chapter cụ thể
            Chapter chapter = chapterRepository.findByIdWithStory(request.getChapterId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy chapter với ID: " + request.getChapterId()));

            // Kiểm tra chapter có thuộc story không
            if (!chapter.getStory().getId().equals(request.getStoryId())) {
                throw new BadRequestException("Chapter không thuộc story được chỉ định");
            }

            chaptersToLock.add(chapter);
        } else if (request.isRangeChapter()) {
            // Khóa range chapter từ chapterStart đến chapterEnd
            chaptersToLock = getChaptersByRange(request.getStoryId(), request.getChapterStart(),
                    request.getChapterEnd());

            if (chaptersToLock.isEmpty()) {
                throw new ResourceNotFoundException("Không tìm thấy chapter nào trong khoảng " +
                        request.getChapterStart() + " - " + request.getChapterEnd());
            }
        }

        // Tối ưu: Check batch tất cả chapter đã khóa trước
        List<Long> chapterIds = chaptersToLock.stream().map(Chapter::getId).collect(Collectors.toList());
        Set<Long> lockedChapterIds = chapterPaymentRepository.findLockedChapterIds(chapterIds)
                .stream().collect(Collectors.toSet());

        List<ChapterPaymentResponse> successfulLocks = new ArrayList<>();
        List<ChapterBatchLockResponse.ChapterLockFailure> failures = new ArrayList<>();

        for (Chapter chapter : chaptersToLock) {
            try {
                // Bỏ qua chapter đã khóa
                if (lockedChapterIds.contains(chapter.getId())) {
                    log.debug("Bỏ qua chapter đã khóa: chapterId={}, chapterNumber={}",
                            chapter.getId(), chapter.getChapterNumber());
                    continue;
                }

                // Tạo record khóa chapter
                int insertedRows = chapterPaymentRepository.insertChapterPayment(
                        chapter.getId(),
                        story.getId(),
                        request.getPrice(),
                        request.getIsVipOnly() != null ? request.getIsVipOnly() : false,
                        true);

                if (insertedRows > 0) {
                    // Lấy lại entity sau khi insert bằng method có sẵn
                    ChapterPayment savedPayment = chapterPaymentRepository.findByChapterIdWithDetails(chapter.getId())
                            .orElseThrow(() -> new BadRequestException("Không thể lấy thông tin payment sau khi tạo"));

                    successfulLocks.add(mapToResponse(savedPayment));
                    log.info("Đã khóa chapter thành công: chapterId={}, chapterNumber={}",
                            chapter.getId(), chapter.getChapterNumber());
                } else {
                    failures.add(new ChapterBatchLockResponse.ChapterLockFailure(
                            chapter.getId(), chapter.getChapterNumber(), chapter.getTitle(),
                            "Không thể tạo payment setting"));
                }

            } catch (Exception e) {
                log.error("Lỗi khi khóa chapter {}: {}", chapter.getId(), e.getMessage());
                failures.add(new ChapterBatchLockResponse.ChapterLockFailure(
                        chapter.getId(), chapter.getChapterNumber(), chapter.getTitle(),
                        "Lỗi hệ thống: " + e.getMessage()));
            }
        }

        // Tạo response
        ChapterBatchLockResponse response = new ChapterBatchLockResponse();
        response.setStoryId(story.getId());
        response.setStoryTitle(story.getTitle());
        response.setStorySlug(story.getSlug());
        response.setTotalChaptersProcessed(chaptersToLock.size());
        response.setSuccessCount(successfulLocks.size());
        response.setFailureCount(failures.size());
        response.setSuccessfulLocks(successfulLocks);
        response.setFailures(failures);

        log.info("Hoàn thành khóa batch chapter: total={}, success={}, failure={}, skipped={}",
                chaptersToLock.size(), successfulLocks.size(), failures.size(),
                chaptersToLock.size() - successfulLocks.size() - failures.size());

        return response;
    }

    @Override
    public String startAsyncBatchLock(ChapterBatchLockRequest request, Long userId) {
        String jobId = UUID.randomUUID().toString();
        log.info("Khởi tạo async batch lock: jobId={}, storyId={}, userId={}", jobId, request.getStoryId(), userId);

        // Validation range size cho async
        if (request.isRangeChapter()) {
            int rangeSize = request.getChapterEnd() - request.getChapterStart() + 1;
            if (rangeSize < 50) {
                throw new BadRequestException("Async chỉ dành cho range >= 50 chapter");
            }
            if (rangeSize > 1000) {
                throw new BadRequestException("Không thể khóa quá 1000 chapter cùng lúc");
            }
        }

        // Tạo initial response
        ChapterBatchLockResponse initialResponse = new ChapterBatchLockResponse();
        initialResponse.setJobId(jobId);
        initialResponse.setStatus("PROCESSING");
        initialResponse.setStartTime(LocalDateTime.now());
        asyncJobResults.put(jobId, initialResponse);

        // Bắt đầu xử lý async
        lockChaptersBatchAsync(request, userId, jobId);

        return jobId;
    }

    @Async("taskExecutor")
    private void lockChaptersBatchAsync(ChapterBatchLockRequest request, Long userId, String jobId) {
        log.info("Bắt đầu async batch lock: jobId={}, storyId={}, userId={}", jobId, request.getStoryId(), userId);

        try {
            // Xử lý async với chunks để tránh transaction quá lớn
            ChapterBatchLockResponse response = processLargeBatchInChunks(request, userId, jobId);

            // Lưu kết quả cuối cùng
            response.setJobId(jobId);
            response.setStatus("COMPLETED");
            response.setEndTime(LocalDateTime.now());
            asyncJobResults.put(jobId, response);

            log.info("Hoàn thành async batch lock: jobId={}, success={}, failure={}",
                    jobId, response.getSuccessCount(), response.getFailureCount());

        } catch (Exception e) {
            log.error("Lỗi async batch lock: jobId={}", jobId, e);

            ChapterBatchLockResponse errorResponse = new ChapterBatchLockResponse();
            errorResponse.setJobId(jobId);
            errorResponse.setStatus("FAILED");
            errorResponse.setEndTime(LocalDateTime.now());
            errorResponse.setFailureCount(1);
            errorResponse.setSuccessCount(0);

            asyncJobResults.put(jobId, errorResponse);
        }
    }

    @Override
    public Optional<ChapterBatchLockResponse> getAsyncJobStatus(String jobId) {
        return Optional.ofNullable(asyncJobResults.get(jobId));
    }

    /**
     * Xử lý batch lớn bằng cách chia thành chunks nhỏ
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private ChapterBatchLockResponse processLargeBatchInChunks(ChapterBatchLockRequest request, Long userId,
            String jobId) {
        final int CHUNK_SIZE = 50; // Xử lý 50 chapter mỗi lần

        // Tạo initial response để lưu progress
        ChapterBatchLockResponse initialResponse = new ChapterBatchLockResponse();
        initialResponse.setJobId(jobId);
        initialResponse.setStatus("PROCESSING");
        initialResponse.setStartTime(LocalDateTime.now());
        asyncJobResults.put(jobId, initialResponse);

        List<ChapterPaymentResponse> allSuccessfulLocks = new ArrayList<>();
        List<ChapterBatchLockResponse.ChapterLockFailure> allFailures = new ArrayList<>();

        int start = request.getChapterStart();
        int end = request.getChapterEnd();
        int totalChapters = end - start + 1;

        // Lấy story info
        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Không tìm thấy story với ID: " + request.getStoryId()));

        // Xử lý từng chunk
        for (int chunkStart = start; chunkStart <= end; chunkStart += CHUNK_SIZE) {
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
                allSuccessfulLocks.addAll(chunkResponse.getSuccessfulLocks());
                allFailures.addAll(chunkResponse.getFailures());

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
                progressResponse.setTotalChaptersProcessed(processed);
                progressResponse.setSuccessCount(allSuccessfulLocks.size());
                progressResponse.setFailureCount(allFailures.size());

                asyncJobResults.put(jobId, progressResponse);

            } catch (Exception e) {
                log.error("Lỗi xử lý chunk {}-{}: jobId={}", chunkStart, chunkEnd, jobId, e);

                // Thêm failure cho toàn bộ chunk
                for (int i = chunkStart; i <= chunkEnd; i++) {
                    allFailures.add(new ChapterBatchLockResponse.ChapterLockFailure(
                            null, i, "Chapter " + i, "Lỗi chunk: " + e.getMessage()));
                }
            }
        }

        // Tạo response cuối cùng
        ChapterBatchLockResponse finalResponse = new ChapterBatchLockResponse();
        finalResponse.setJobId(jobId);
        finalResponse.setStoryId(story.getId());
        finalResponse.setStoryTitle(story.getTitle());
        finalResponse.setStorySlug(story.getSlug());
        finalResponse.setTotalChaptersProcessed(totalChapters);
        finalResponse.setSuccessCount(allSuccessfulLocks.size());
        finalResponse.setFailureCount(allFailures.size());
        finalResponse.setSuccessfulLocks(allSuccessfulLocks);
        finalResponse.setFailures(allFailures);
        finalResponse.setStartTime(initialResponse.getStartTime());

        return finalResponse;
    }

    /**
     * Xử lý một chunk với transaction riêng biệt
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private ChapterBatchLockResponse processChunkSeparately(ChapterBatchLockRequest request, Long userId) {
        return lockChaptersBatch(request, userId);
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

        // Thông tin story
        if (payment.getStory() != null) {
            response.setStoryTitle(payment.getStory().getTitle());
            response.setStorySlug(payment.getStory().getSlug());
        }

        return response;
    }
}
