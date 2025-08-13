package com.meobeo.truyen.controller.story;

import com.meobeo.truyen.domain.response.story.AnalyticsSeriesResponse;
import com.meobeo.truyen.domain.response.story.TopStoriesResponse;
import com.meobeo.truyen.service.interfaces.StoryViewsService;
import com.meobeo.truyen.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Controller xử lý analytics cho story views
 * Cung cấp các endpoint để lấy thống kê views theo ngày/tuần/tháng và top
 * stories
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class StoryAnalyticsController {

    private final StoryViewsService storyViewsService;

    /**
     * GET /api/v1/analytics/stories/{storyId}/daily - Lấy thống kê views theo ngày
     * Có cache cho các khoảng thời gian phổ biến (7 ngày)
     */
    @GetMapping("/stories/{storyId}/daily")
    public ResponseEntity<ApiResponse<AnalyticsSeriesResponse>> getDaily(
            @PathVariable Long storyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        log.info("API lấy daily analytics được gọi: storyId={}, start={}, end={}", storyId, start, end);
        var data = storyViewsService.getDailyViews(storyId, start, end);
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê theo ngày thành công", data));
    }

    /**
     * GET /api/v1/analytics/stories/{storyId}/weekly - Lấy thống kê views theo tuần
     * Có cache cho các khoảng thời gian phổ biến (4 tuần)
     */
    @GetMapping("/stories/{storyId}/weekly")
    public ResponseEntity<ApiResponse<AnalyticsSeriesResponse>> getWeekly(
            @PathVariable Long storyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        log.info("API lấy weekly analytics được gọi: storyId={}, start={}, end={}", storyId, start, end);
        var data = storyViewsService.getWeeklyViews(storyId, start, end);
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê theo tuần thành công", data));
    }

    /**
     * GET /api/v1/analytics/stories/{storyId}/monthly - Lấy thống kê views theo
     * tháng
     * Có cache cho các khoảng thời gian phổ biến (12 tháng)
     */
    @GetMapping("/stories/{storyId}/monthly")
    public ResponseEntity<ApiResponse<AnalyticsSeriesResponse>> getMonthly(
            @PathVariable Long storyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        log.info("API lấy monthly analytics được gọi: storyId={}, start={}, end={}", storyId, start, end);
        var data = storyViewsService.getMonthlyViews(storyId, start, end);
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê theo tháng thành công", data));
    }

    /**
     * GET /api/v1/analytics/top-stories - Lấy danh sách top stories theo views
     * Có cache cho các khoảng thời gian phổ biến (7/30 ngày)
     */
    @GetMapping("/top-stories")
    public ResponseEntity<ApiResponse<TopStoriesResponse>> getTopStories(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("API lấy top stories được gọi: start={}, end={}, page={}, size={}", start, end, page, size);
        var data = storyViewsService.getTopStories(start, end, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách top stories thành công", data));
    }
}
