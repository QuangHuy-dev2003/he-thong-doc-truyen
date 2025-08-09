package com.meobeo.truyen.domain.response.chapter;

import lombok.Data;

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
}
