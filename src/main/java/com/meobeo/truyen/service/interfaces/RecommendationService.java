package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.recommendation.CreateRecommendationRequest;
import com.meobeo.truyen.domain.response.recommendation.RecommendationListResponse;
import com.meobeo.truyen.domain.response.recommendation.RecommendationResponse;
import com.meobeo.truyen.domain.response.recommendation.TopRecommendedStoriesResponse;
import org.springframework.data.domain.Pageable;

public interface RecommendationService {

    /**
     * Tạo đề cử truyện
     */
    RecommendationResponse createRecommendation(CreateRecommendationRequest request, Long userId);

    /**
     * Lấy danh sách đề cử của user
     */
    RecommendationListResponse getUserRecommendations(Long userId, Pageable pageable);

    /**
     * Lấy danh sách đề cử của story
     */
    RecommendationListResponse getStoryRecommendations(Long storyId, Pageable pageable);

    /**
     * Lấy top stories được đề cử nhiều nhất
     */
    TopRecommendedStoriesResponse getTopRecommendedStories(Pageable pageable);

    /**
     * Lấy số lượng đề cử của story
     */
    long getRecommendationCount(Long storyId);

    /**
     * Kiểm tra user đã đề cử story này chưa
     */
    boolean hasUserRecommendedStory(Long userId, Long storyId);
}
