package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.Chapter;
import com.meobeo.truyen.domain.entity.ChapterPayment;
import com.meobeo.truyen.domain.request.chapter.ChapterLockRequest;
import com.meobeo.truyen.domain.response.chapter.ChapterPaymentResponse;
import com.meobeo.truyen.exception.ForbiddenException;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.repository.ChapterPaymentRepository;
import com.meobeo.truyen.repository.ChapterRepository;
import com.meobeo.truyen.repository.StoryRepository;
import com.meobeo.truyen.service.interfaces.ChapterPaymentService;
import com.meobeo.truyen.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterPaymentServiceImpl implements ChapterPaymentService {

    private final ChapterPaymentRepository chapterPaymentRepository;
    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;
    private final SecurityUtils securityUtils;

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

        // Tạo hoặc cập nhật payment setting
        ChapterPayment chapterPayment = chapterPaymentRepository.findByChapterId(chapterId)
                .orElse(new ChapterPayment());

        chapterPayment.setChapterId(chapterId);
        chapterPayment.setStoryId(chapter.getStory().getId());
        chapterPayment.setPrice(request.getPrice());
        chapterPayment.setIsVipOnly(request.getIsVipOnly() != null ? request.getIsVipOnly() : false);
        chapterPayment.setIsLocked(true);
        chapterPayment.setChapter(chapter);
        chapterPayment.setStory(chapter.getStory());

        ChapterPayment savedPayment = chapterPaymentRepository.save(chapterPayment);

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
