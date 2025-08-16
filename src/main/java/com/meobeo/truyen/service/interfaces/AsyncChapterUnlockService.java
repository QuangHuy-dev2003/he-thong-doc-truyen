package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.chapter.UnlockChapterRangeRequest;
import com.meobeo.truyen.domain.request.chapter.UnlockFullStoryRequest;
import com.meobeo.truyen.domain.response.chapter.UnlockChapterBatchResponse;
import com.meobeo.truyen.domain.response.chapter.UnlockFullStoryResponse;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public interface AsyncChapterUnlockService {

    /**
     * Xử lý mở khóa range chương bất đồng bộ
     * 
     * @param storyId ID truyện
     * @param request Thông tin mở khóa range
     * @param userId  ID người dùng
     * @param jobId   Job ID để track progress
     */
    void processUnlockRangeAsync(Long storyId, UnlockChapterRangeRequest request, Long userId, String jobId);

    /**
     * Xử lý mở khóa full truyện bất đồng bộ
     * 
     * @param storyId ID truyện
     * @param request Thông tin mở khóa full truyện
     * @param userId  ID người dùng
     * @param jobId   Job ID để track progress
     */
    void processUnlockFullStoryAsync(Long storyId, UnlockFullStoryRequest request, Long userId, String jobId);

    /**
     * Lấy map các job unlock range đang chạy
     */
    Map<String, UnlockChapterBatchResponse> getUnlockRangeJobs();

    /**
     * Lấy map các job unlock full story đang chạy
     */
    Map<String, UnlockFullStoryResponse> getUnlockFullStoryJobs();

    /**
     * Lấy map userId của từng job
     */
    Map<String, Long> getJobUserMap();

    /**
     * Lấy map cancel flag của từng job
     */
    Map<String, AtomicBoolean> getCancelFlags();

    /**
     * Khởi tạo job tracking
     */
    void initializeRangeJob(String jobId, Long userId);

    /**
     * Khởi tạo job tracking cho full story
     */
    void initializeFullStoryJob(String jobId, Long userId);
}
