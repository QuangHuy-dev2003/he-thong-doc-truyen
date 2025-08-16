package com.meobeo.truyen.domain.response.chapter;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UnlockChapterBatchResponse {

    // Job tracking fields
    private String jobId;
    private String status;
    private String message;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Response data
    private Long storyId;
    private String storyTitle;
    private Integer totalChaptersUnlocked;
    private Integer totalSpiritStonesSpent;
    private Integer totalOriginalPrice;
    private Integer totalDiscountedPrice;
    private Double totalDiscountPercent;
    private LocalDateTime unlockedAt;
}
