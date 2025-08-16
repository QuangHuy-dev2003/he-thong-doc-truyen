package com.meobeo.truyen.domain.response.chapter;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UnlockFullStoryResponse {

    private Long storyId;
    private String storyTitle;
    private Integer totalChaptersUnlocked;
    private Integer totalSpiritStonesSpent;
    private Integer totalOriginalPrice;
    private Integer totalDiscountedPrice;
    private Double totalDiscountPercent;
    private String description;
    private LocalDateTime unlockedAt;
}
