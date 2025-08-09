package com.meobeo.truyen.domain.response.chapter;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChapterBatchLockResponse {

    private Long storyId;
    private String storyTitle;
    private String storySlug;
    private Integer totalChaptersProcessed;
    private Integer successCount;
    private Integer failureCount;

    // Danh sách chapter đã khóa thành công
    private List<ChapterPaymentResponse> successfulLocks;

    // Danh sách chapter không thể khóa (đã bị khóa rồi hoặc lỗi khác)
    private List<ChapterLockFailure> failures;

    // Thông tin async job (nếu là async processing)
    private String jobId;
    private String status; // "PROCESSING", "COMPLETED", "FAILED"
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Data
    public static class ChapterLockFailure {
        private Long chapterId;
        private Integer chapterNumber;
        private String chapterTitle;
        private String reason;

        public ChapterLockFailure(Long chapterId, Integer chapterNumber, String chapterTitle, String reason) {
            this.chapterId = chapterId;
            this.chapterNumber = chapterNumber;
            this.chapterTitle = chapterTitle;
            this.reason = reason;
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
