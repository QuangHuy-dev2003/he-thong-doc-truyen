package com.meobeo.truyen.domain.response.chapter;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UnlockChapterResponse {

    private Long chapterId;
    private String chapterTitle;
    private Integer spiritStonesSpent;
    private Integer originalPrice;
    private Integer discountedPrice;
    private Double discountPercent;
    private String description;
    private LocalDateTime unlockedAt;
}
