package com.meobeo.truyen.domain.response.chapter;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UnlockChapterBatchResponse {

    private Long storyId;
    private String storyTitle;
    private List<UnlockedChapterInfo> unlockedChapters;
    private Integer totalSpiritStonesSpent;
    private Integer totalOriginalPrice;
    private Integer totalDiscountedPrice;
    private Double totalDiscountPercent;
    private String description;
    private LocalDateTime unlockedAt;

    @Data
    public static class UnlockedChapterInfo {
        private Long chapterId;
        private String chapterTitle;
        private Integer chapterNumber;
        private Integer spiritStonesSpent;
        private Integer originalPrice;
        private Integer discountedPrice;
    }
}
