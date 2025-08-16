package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.chapter.ChapterBatchLockRequest;

public interface AsyncChapterPaymentService {

    // startAsyncBatchLock đã được chuyển về ChapterPaymentService

    /**
     * Khởi tạo job tracking
     */
    void initializeJob(String jobId, Long userId);

    /**
     * Xử lý batch lock bất đồng bộ (được gọi từ ChapterPaymentService)
     */
    void processBatchLockAsyncInternal(com.meobeo.truyen.domain.request.chapter.ChapterBatchLockRequest request,
            Long userId, String jobId);

    /**
     * Lấy trạng thái job async
     */
    java.util.Optional<com.meobeo.truyen.domain.response.chapter.ChapterBatchLockResponse> getAsyncJobStatus(
            String jobId);

    /**
     * Hủy job async
     */
    boolean cancelAsyncJob(String jobId, Long userId);
}
