package com.meobeo.truyen.domain.response.chapter;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UnlockFullStoryResponse {
    private String jobId;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
    private String message;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Thông tin story
    private Long storyId;
    private String storyTitle;

    // Thông tin unlock
    private Integer totalChaptersUnlocked;
    private Integer totalChaptersToUnlock; // Tổng số chapter cần unlock
    private Integer processedChapters; // Số chapter đã xử lý
    private Integer currentBatch; // Batch hiện tại đang xử lý
    private Integer totalBatches; // Tổng số batch

    // Thông tin giá
    private Integer totalSpiritStonesSpent;
    private Integer totalOriginalPrice;
    private Integer totalDiscountedPrice;
    private Double totalDiscountPercent;

    // Thời gian unlock
    private LocalDateTime unlockedAt;

    // Progress tracking
    private Double progressPercent; // Phần trăm hoàn thành (0-100)
    private String currentBatchInfo; // Thông tin batch hiện tại
}
