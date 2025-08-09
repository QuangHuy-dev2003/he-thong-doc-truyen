package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.chapter.ChapterLockRequest;
import com.meobeo.truyen.domain.response.chapter.ChapterPaymentResponse;

import java.util.List;

public interface ChapterPaymentService {

    /**
     * Khóa chapter và đặt giá tiền
     * Chỉ ADMIN và UPLOADER có quyền thực hiện
     */
    ChapterPaymentResponse lockChapter(Long chapterId, ChapterLockRequest request, Long userId);

    /**
     * Mở khóa chapter (đặt isLocked = false)
     * Chỉ ADMIN và UPLOADER có quyền thực hiện
     */
    ChapterPaymentResponse unlockChapter(Long chapterId, Long userId);

    /**
     * Cập nhật thông tin payment của chapter
     * Chỉ ADMIN và UPLOADER có quyền thực hiện
     */
    ChapterPaymentResponse updateChapterPayment(Long chapterId, ChapterLockRequest request, Long userId);

    /**
     * Lấy thông tin payment của chapter
     */
    ChapterPaymentResponse getChapterPaymentInfo(Long chapterId);

    /**
     * Lấy danh sách chapter payments của story
     * Chỉ ADMIN hoặc author của story mới được xem
     */
    List<ChapterPaymentResponse> getChapterPaymentsByStory(Long storyId, Long userId);

    /**
     * Kiểm tra chapter có bị khóa không
     */
    boolean isChapterLocked(Long chapterId);

    /**
     * Kiểm tra user có quyền chỉnh sửa payment settings không
     */
    boolean canManageChapterPayment(Long chapterId, Long userId);

    /**
     * Kiểm tra user có quyền quản lý story không (chỉ author hoặc admin)
     */
    boolean canManageStory(Long storyId, Long userId);

    /**
     * Xóa payment setting của chapter (để chapter trở thành miễn phí)
     */
    void removeChapterPayment(Long chapterId, Long userId);
}
