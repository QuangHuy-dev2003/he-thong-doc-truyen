package com.meobeo.truyen.domain.response.recommendation;

import com.meobeo.truyen.domain.enums.RecommendationType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecommendationResponse {

    private Long id;
    private Long userId;
    private String username;
    private String displayName;
    private Long storyId;
    private String storyTitle;
    private String storySlug;
    private RecommendationType recommendationType;
    private LocalDateTime createdAt;
    private String message;
}
