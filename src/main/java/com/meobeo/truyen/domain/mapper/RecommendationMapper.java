package com.meobeo.truyen.domain.mapper;

import com.meobeo.truyen.domain.entity.StoryRecommendation;
import com.meobeo.truyen.domain.request.recommendation.CreateRecommendationRequest;
import com.meobeo.truyen.domain.response.recommendation.RecommendationResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RecommendationMapper {

    /**
     * Chuyển đổi từ CreateRecommendationRequest thành StoryRecommendation entity
     */
    public StoryRecommendation toEntity(CreateRecommendationRequest request, Long userId) {
        StoryRecommendation entity = new StoryRecommendation();
        entity.setUserId(userId);
        entity.setStoryId(request.getStoryId());
        entity.setRecommendationType(request.getRecommendationType());
        entity.setMessage(request.getMessage());
        return entity;
    }

    /**
     * Chuyển đổi từ StoryRecommendation entity thành RecommendationResponse
     */
    public RecommendationResponse toResponse(StoryRecommendation entity) {
        RecommendationResponse response = new RecommendationResponse();
        response.setId(entity.getId());
        response.setUserId(entity.getUserId());
        response.setStoryId(entity.getStoryId());
        response.setRecommendationType(entity.getRecommendationType());
        response.setCreatedAt(entity.getCreatedAt());
        response.setMessage(entity.getMessage());

        // Lấy thông tin user nếu có
        if (entity.getUser() != null) {
            response.setUsername(entity.getUser().getUsername());
            response.setDisplayName(entity.getUser().getDisplayName());
        }

        // Lấy thông tin story nếu có
        if (entity.getStory() != null) {
            response.setStoryTitle(entity.getStory().getTitle());
            response.setStorySlug(entity.getStory().getSlug());
        }

        return response;
    }

    /**
     * Chuyển đổi danh sách StoryRecommendation thành danh sách
     * RecommendationResponse
     */
    public List<RecommendationResponse> toResponseList(List<StoryRecommendation> entities) {
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
