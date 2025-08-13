package com.meobeo.truyen.domain.response.favorite;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FavoriteResponse {

    private Long storyId;
    private String storyTitle;
    private String storySlug;
    private String storyCoverImageUrl;
    private String authorName;
    private String authorUsername;
    private LocalDateTime favoritedAt;
    private Long chapterCount;
    private Long viewCount;
    private Long favoriteCount;
    private Double averageRating;
}
