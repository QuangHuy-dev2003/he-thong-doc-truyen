package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.chapter.ChapterBatchLockRequest;
import com.meobeo.truyen.domain.request.chapter.ChapterLockRequest;
import com.meobeo.truyen.domain.response.chapter.ChapterBatchLockResponse;
import com.meobeo.truyen.domain.response.chapter.ChapterPaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

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
     * Lấy danh sách chapter payments của story (có phân trang)
     * Chỉ ADMIN hoặc author của story mới được xem
     */
    Page<ChapterPaymentResponse> getChapterPaymentsByStory(Long storyId, Long userId, Pageable pageable);

    /**
     * Lấy danh sách chapter payments của story (không phân trang - deprecated)
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

    /**
     * Khóa nhiều chapter cùng lúc (batch lock)
     * Hỗ trợ khóa 1 chapter cụ thể hoặc khóa theo range chapter number
     */
    ChapterBatchLockResponse lockChaptersBatch(ChapterBatchLockRequest request, Long userId);

    /**
     * Khóa nhiều chapter bất đồng bộ (cho range lớn > 50 chapters)
     * Trả về jobId để track progress
     */
    String startAsyncBatchLock(ChapterBatchLockRequest request, Long userId);

    /**
     * Lấy trạng thái job async batch lock
     */
    Optional<ChapterBatchLockResponse> getAsyncJobStatus(String jobId);
}
