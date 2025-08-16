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
import com.meobeo.truyen.service.interfaces.AsyncChapterPaymentService;
import com.meobeo.truyen.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterPaymentServiceImpl implements ChapterPaymentService {

    private final ChapterPaymentRepository chapterPaymentRepository;
    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;
    private final SecurityUtils securityUtils;

    private final AsyncChapterPaymentService asyncChapterPaymentService;

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

        // Sử dụng native SQL INSERT với ON CONFLICT DO NOTHING để tránh optimistic
        // locking conflict
        // Không cần check existsByChapterId vì ON CONFLICT sẽ handle duplicate key
        log.info("Chapter {} chưa bị khóa, tiến hành tạo record mới", chapterId);

        int insertedRows = chapterPaymentRepository.insertChapterPaymentIgnoreDuplicate(
                chapterId,
                chapter.getStory().getId(),
                request.getPrice(),
                request.getIsVipOnly() != null ? request.getIsVipOnly() : false,
                true // isLocked = true
        );

        if (insertedRows == 0) {
            log.error("Chapter {} đã bị khóa rồi hoặc không thể tạo payment setting!", chapterId);
            throw new BadRequestException("Chapter đã bị khóa rồi");
        }

        log.info("Đã tạo payment setting thành công: chapterId={}", chapterId);

        log.info("Đã khóa chapter thành công: chapterId={}, price={}", chapterId, request.getPrice());

        // Tạo response trực tiếp thay vì lấy lại entity để tránh transaction conflict
        ChapterPaymentResponse response = new ChapterPaymentResponse();
        response.setChapterId(chapterId);
        response.setStoryId(chapter.getStory().getId());
        response.setPrice(request.getPrice());
        response.setIsVipOnly(request.getIsVipOnly() != null ? request.getIsVipOnly() : false);
        response.setIsLocked(true);
        response.setChapterNumber(chapter.getChapterNumber());
        response.setChapterTitle(chapter.getTitle());
        response.setChapterSlug(chapter.getSlug());
        response.setStoryTitle(chapter.getStory().getTitle());
        response.setStorySlug(chapter.getStory().getSlug());

        return response;
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
    public Page<ChapterPaymentResponse> getChapterPaymentsByStory(Long storyId, Long userId, Pageable pageable) {
        log.info("Lấy danh sách payment của story (có phân trang): storyId={}, userId={}, page={}, size={}",
                storyId, userId, pageable.getPageNumber(), pageable.getPageSize());

        // Kiểm tra quyền trước khi cho phép xem danh sách payments
        if (!canManageStory(storyId, userId)) {
            throw new ForbiddenException("Bạn không có quyền xem thông tin payment của story này");
        }

        Page<ChapterPayment> paymentsPage = chapterPaymentRepository.findByStoryIdOrderByChapterNumber(storyId,
                pageable);

        // Map kết quả với thông tin chapter và story đầy đủ
        Page<ChapterPaymentResponse> responsePage = paymentsPage.map(payment -> {
            // Lấy thông tin chapter và story đầy đủ
            ChapterPayment fullPayment = chapterPaymentRepository.findByChapterIdWithDetails(payment.getChapterId())
                    .orElse(payment);
            return mapToResponse(fullPayment);
        });

        return responsePage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChapterPaymentResponse> getChapterPaymentsByStory(Long storyId, Long userId) {
        log.info("Lấy danh sách payment của story (không phân trang): storyId={}, userId={}", storyId, userId);

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
        List<String> failureReasons = new ArrayList<>();

        for (Chapter chapter : chaptersToLock) {
            try {
                // Bỏ qua chapter đã khóa
                if (lockedChapterIds.contains(chapter.getId())) {
                    log.debug("Bỏ qua chapter đã khóa: chapterId={}, chapterNumber={}",
                            chapter.getId(), chapter.getChapterNumber());
                    continue;
                }

                // Tạo record khóa chapter bằng entity
                try {
                    ChapterPayment payment = new ChapterPayment();
                    payment.setChapterId(chapter.getId());
                    payment.setStoryId(story.getId());
                    payment.setPrice(request.getPrice());
                    payment.setIsVipOnly(request.getIsVipOnly() != null ? request.getIsVipOnly() : false);
                    payment.setIsLocked(true);

                    ChapterPayment savedPayment = chapterPaymentRepository.save(payment);
                    successfulLocks.add(mapToResponse(savedPayment));
                    log.info("Đã khóa chapter thành công: chapterId={}, chapterNumber={}",
                            chapter.getId(), chapter.getChapterNumber());
                } catch (Exception e) {
                    log.error("Lỗi khi tạo payment setting cho chapter {}: {}", chapter.getId(), e.getMessage());
                    failureReasons.add("Lỗi tạo payment setting: " + e.getMessage());
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
        response.setSkippedCount(0);

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

        log.info("Hoàn thành khóa batch chapter: total={}, success={}, failure={}, skipped={}",
                chaptersToLock.size(), successfulLocks.size(), failureReasons.size(),
                chaptersToLock.size() - successfulLocks.size() - failureReasons.size());

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
        }

        // Khởi tạo job tracking trong async service
        asyncChapterPaymentService.initializeJob(jobId, userId);

        // Gọi async service để xử lý bất đồng bộ
        asyncChapterPaymentService.processBatchLockAsyncInternal(request, userId, jobId);

        log.info("Đã trả về jobId ngay lập tức: {}", jobId);
        return jobId;
    }

    @Override
    public Optional<ChapterBatchLockResponse> getAsyncJobStatus(String jobId) {
        return asyncChapterPaymentService.getAsyncJobStatus(jobId);
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
