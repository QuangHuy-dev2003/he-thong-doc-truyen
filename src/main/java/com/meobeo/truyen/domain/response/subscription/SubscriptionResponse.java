package com.meobeo.truyen.domain.response.subscription;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubscriptionResponse {

    private Long storyId;
    private String storyTitle;
    private String storySlug;
    private String storyCoverImageUrl;
    private String authorName;
    private String authorUsername;
    private LocalDateTime subscribedAt;
    private Boolean isActive;
    private Long chapterCount;
    private Long viewCount;
    private Long favoriteCount;
    private Double averageRating;
}
