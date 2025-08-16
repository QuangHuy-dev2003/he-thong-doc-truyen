package com.meobeo.truyen.domain.response.chapter;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChapterBatchLockResponse {

    private Long storyId;
    private String storyTitle;
    private String storySlug;

    // Thống kê tổng quan
    private Integer totalChaptersRequested; // Tổng số chapter yêu cầu khóa
    private Integer totalChaptersProcessed; // Số chapter đã xử lý
    private Integer successCount; // Số chapter khóa thành công
    private Integer failureCount; // Số chapter khóa thất bại
    private Integer skippedCount; // Số chapter đã bị khóa trước đó

    // Tiến độ xử lý (dạng "400/1000")
    private String progress; // Format: "400/1000"
    private Integer progressPercentage; // Phần trăm hoàn thành (0-100)

    // Chỉ hiển thị lỗi chính xác (không phải danh sách chapter)
    private String mainError; // Lỗi chính nếu có
    private List<String> errorMessages; // Danh sách các lỗi khác nhau (không trùng lặp)

    // Thông tin async job (nếu là async processing)
    private String jobId;
    private String status; // "PROCESSING", "COMPLETED", "FAILED"
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Helper methods để tính toán thống kê
    public void calculateProgress() {
        if (totalChaptersRequested != null && totalChaptersRequested > 0) {
            this.progress = totalChaptersProcessed + "/" + totalChaptersRequested;
            this.progressPercentage = (int) Math.round((double) totalChaptersProcessed / totalChaptersRequested * 100);
        } else {
            this.progress = "0/0";
            this.progressPercentage = 0;
        }
    }

    public void addErrorMessage(String errorMessage) {
        if (this.errorMessages == null) {
            this.errorMessages = new java.util.ArrayList<>();
        }
        if (!this.errorMessages.contains(errorMessage)) {
            this.errorMessages.add(errorMessage);
        }
    }

    @Data
    public static class AsyncBatchLockInfo {
        private String jobId;
        private String status; // "PROCESSING", "COMPLETED", "FAILED"
        private String message;
        private LocalDateTime startTime;
        private Integer totalChapters;
        private Integer estimatedTimeMinutes;

        public AsyncBatchLockInfo(String jobId, String message, Integer totalChapters) {
            this.jobId = jobId;
            this.status = "PROCESSING";
            this.message = message;
            this.totalChapters = totalChapters;
            this.startTime = LocalDateTime.now();
            this.estimatedTimeMinutes = Math.max(1, totalChapters / 100); // Ước tính 1 phút cho 100 chapter
        }
    }
}
