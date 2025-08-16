package com.meobeo.truyen.controller.user;

import com.meobeo.truyen.domain.request.recommendation.CreateRecommendationRequest;
import com.meobeo.truyen.domain.response.recommendation.RecommendationListResponse;
import com.meobeo.truyen.domain.response.recommendation.RecommendationResponse;
import com.meobeo.truyen.domain.response.recommendation.TopRecommendedStoriesResponse;
import com.meobeo.truyen.service.interfaces.RecommendationService;
import com.meobeo.truyen.utils.ApiResponse;
import com.meobeo.truyen.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class UserRecommendationController {

    private final RecommendationService recommendationService;
    private final SecurityUtils securityUtils;

    /**
     * Tạo đề cử truyện
     */
    @PostMapping("/recommendations/create")
    public ResponseEntity<ApiResponse<RecommendationResponse>> createRecommendation(
            @Valid @RequestBody CreateRecommendationRequest request) {
        Long userId = securityUtils.getCurrentUserIdOrThrow();
        log.info("User {} tạo đề cử cho story {}", userId, request.getStoryId());

        RecommendationResponse response = recommendationService.createRecommendation(request, userId);

        return ResponseEntity.ok(ApiResponse.success("Tạo đề cử thành công", response));
    }

    /**
     * Lấy lịch sử đề cử của bản thân
     */
    @GetMapping("/recommendations/my-recommendations")
    public ResponseEntity<ApiResponse<RecommendationListResponse>> getMyRecommendations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = securityUtils.getCurrentUserIdOrThrow();
        log.info("User {} lấy lịch sử đề cử, page: {}, size: {}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        RecommendationListResponse response = recommendationService.getUserRecommendations(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử đề cử thành công", response));
    }

    /**
     * Xem đề cử của story
     */
    @GetMapping("/recommendations/story/{storyId}")
    public ResponseEntity<ApiResponse<RecommendationListResponse>> getStoryRecommendations(
            @PathVariable Long storyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Lấy đề cử của story {}, page: {}, size: {}", storyId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        RecommendationListResponse response = recommendationService.getStoryRecommendations(storyId, pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy đề cử của truyện thành công", response));
    }

    /**
     * Top stories được đề cử nhiều
     */
    @GetMapping("/recommendations/top-stories")
    public ResponseEntity<ApiResponse<TopRecommendedStoriesResponse>> getTopRecommendedStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Lấy top stories được đề cử nhiều, page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        TopRecommendedStoriesResponse response = recommendationService.getTopRecommendedStories(pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy top stories được đề cử thành công", response));
    }
}
