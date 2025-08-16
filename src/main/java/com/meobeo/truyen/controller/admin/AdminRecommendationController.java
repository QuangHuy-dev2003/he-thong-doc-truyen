package com.meobeo.truyen.controller.admin;

import com.meobeo.truyen.domain.response.recommendation.RecommendationListResponse;
import com.meobeo.truyen.domain.response.recommendation.TopRecommendedStoriesResponse;
import com.meobeo.truyen.service.interfaces.RecommendationService;
import com.meobeo.truyen.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminRecommendationController {

    private final RecommendationService recommendationService;

    /**
     * Tất cả đề cử (để thống kê)
     */
    @GetMapping("/recommendations/all")
    public ResponseEntity<ApiResponse<RecommendationListResponse>> getAllRecommendations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin lấy tất cả đề cử, page: {}, size: {}", page, size);

        // TODO: Implement method getAllRecommendations trong RecommendationService
        // Hiện tại tạm thời trả về empty response
        RecommendationListResponse response = new RecommendationListResponse();
        response.setRecommendations(java.util.Collections.emptyList());
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(0);
        response.setTotalPages(0);

        return ResponseEntity.ok(ApiResponse.success("Lấy tất cả đề cử thành công", response));
    }

    /**
     * Thống kê đề cử
     */
    @GetMapping("/recommendations/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecommendationStats() {
        log.info("Admin lấy thống kê đề cử");

        Map<String, Object> stats = new HashMap<>();

        // TODO: Implement các method thống kê trong RecommendationService
        // Hiện tại tạm thời trả về mock data
        stats.put("totalRecommendations", 0L);
        stats.put("totalStoriesRecommended", 0L);
        stats.put("totalUsersWhoRecommended", 0L);
        stats.put("averageRecommendationsPerStory", 0.0);
        stats.put("topRecommendedStoryId", null);
        stats.put("topRecommendedStoryTitle", null);
        stats.put("topRecommendedStoryCount", 0L);

        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê đề cử thành công", stats));
    }
}
