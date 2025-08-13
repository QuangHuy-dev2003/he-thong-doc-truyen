package com.meobeo.truyen.controller.user;

import com.meobeo.truyen.domain.request.reading.RecordReadingRequest;
import com.meobeo.truyen.domain.response.reading.ReadingHistoryListResponse;
import com.meobeo.truyen.domain.response.reading.ReadingHistoryResponse;
import com.meobeo.truyen.domain.response.reading.LastReadChapterResponse;
import com.meobeo.truyen.service.interfaces.ReadingHistoryService;
import com.meobeo.truyen.utils.ApiResponse;
import com.meobeo.truyen.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ReadingHistoryController {

    private final ReadingHistoryService readingHistoryService;
    private final SecurityUtils securityUtils;

    /**
     * POST /api/v1/reading-history/record - Ghi lại lịch sử đọc chapter
     */
    @PostMapping("/reading-history/record")
    public ResponseEntity<ApiResponse<ReadingHistoryResponse>> recordReading(
            @Valid @RequestBody RecordReadingRequest request) {

        log.info("API ghi lại lịch sử đọc được gọi: chapterId={}", request.getChapterId());

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        ReadingHistoryResponse readingHistory = readingHistoryService.recordReading(request.getChapterId(), userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã ghi lại lịch sử đọc thành công", readingHistory));
    }

    /**
     * GET /api/v1/reading-history/my-history - Lấy lịch sử đọc của user hiện tại
     */
    @GetMapping("/reading-history/my-history")
    public ResponseEntity<ApiResponse<ReadingHistoryListResponse>> getMyReadingHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("API lấy lịch sử đọc được gọi: page={}, size={}", page, size);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        PageRequest pageRequest = PageRequest.of(page, size);
        ReadingHistoryListResponse readingHistory = readingHistoryService.getUserReadingHistory(userId, pageRequest);

        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử đọc thành công", readingHistory));
    }

    /**
     * GET /api/v1/reading-history/story/{storyId} - Lấy lịch sử đọc của user trong
     * story
     */
    @GetMapping("/reading-history/story/{storyId}")
    public ResponseEntity<ApiResponse<ReadingHistoryResponse>> getMyReadingHistoryByStory(
            @PathVariable Long storyId) {

        log.info("API lấy lịch sử đọc trong story được gọi: storyId={}", storyId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        ReadingHistoryResponse readingHistory = readingHistoryService.getUserReadingHistoryByStory(userId, storyId);

        if (readingHistory != null) {
            return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử đọc trong story thành công", readingHistory));
        } else {
            return ResponseEntity.ok(ApiResponse.success("Chưa có lịch sử đọc cho truyện này", null));
        }
    }

    /**
     * GET /api/v1/reading-history/last-read/{storyId} - Lấy chapter cuối cùng đã
     * đọc trong story
     */
    @GetMapping("/reading-history/last-read/{storyId}")
    public ResponseEntity<ApiResponse<LastReadChapterResponse>> getLastReadChapter(@PathVariable Long storyId) {

        log.info("API lấy chapter cuối cùng đã đọc được gọi: storyId={}", storyId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        LastReadChapterResponse lastReadChapter = readingHistoryService.getLastReadChapter(userId, storyId);

        if (lastReadChapter != null) {
            return ResponseEntity.ok(ApiResponse.success("Lấy chapter cuối cùng đã đọc thành công", lastReadChapter));
        } else {
            return ResponseEntity.ok(ApiResponse.success("Chưa có lịch sử đọc cho truyện này", null));
        }
    }

    /**
     * GET /api/v1/reading-history/count - Đếm số story đã đọc của user hiện tại
     */
    @GetMapping("/reading-history/count")
    public ResponseEntity<ApiResponse<Long>> getMyReadingCount() {

        log.info("API đếm số story đã đọc được gọi");

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        Long count = readingHistoryService.countUserReadStories(userId);

        return ResponseEntity.ok(ApiResponse.success("Đếm số story đã đọc thành công", count));
    }

    /**
     * DELETE /api/v1/reading-history/remove/{storyId} - Xóa lịch sử đọc của một
     * story
     */
    @DeleteMapping("/reading-history/remove/{storyId}")
    public ResponseEntity<ApiResponse<Void>> deleteReadingHistory(@PathVariable Long storyId) {

        log.info("API xóa lịch sử đọc được gọi: storyId={}", storyId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        readingHistoryService.deleteReadingHistory(userId, storyId);

        return ResponseEntity.ok(ApiResponse.success("Đã xóa lịch sử đọc thành công", null));
    }

    /**
     * DELETE /api/v1/reading-history/clear - Xóa tất cả lịch sử đọc của user
     */
    @DeleteMapping("/reading-history/clear")
    public ResponseEntity<ApiResponse<Void>> clearMyReadingHistory() {

        log.info("API xóa tất cả lịch sử đọc được gọi");

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        readingHistoryService.clearUserReadingHistory(userId);

        return ResponseEntity.ok(ApiResponse.success("Đã xóa tất cả lịch sử đọc thành công", null));
    }

    /**
     * GET /api/v1/reading-history/check/{storyId} - Kiểm tra user đã đọc story chưa
     */
    @GetMapping("/reading-history/check/{storyId}")
    public ResponseEntity<ApiResponse<Boolean>> checkReadingStatus(@PathVariable Long storyId) {

        log.info("API kiểm tra trạng thái đọc được gọi: storyId={}", storyId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        boolean hasRead = readingHistoryService.hasUserReadStory(userId, storyId);

        return ResponseEntity.ok(ApiResponse.success("Kiểm tra trạng thái đọc thành công", hasRead));
    }

    /**
     * GET /api/v1/reading-history/last-read-stories - Lấy danh sách story đã đọc
     * gần đây
     */
    @GetMapping("/reading-history/last-read-stories")
    public ResponseEntity<ApiResponse<ReadingHistoryListResponse>> getLastReadStories() {

        log.info("API lấy danh sách story đã đọc gần đây được gọi");

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        ReadingHistoryListResponse lastReadStories = readingHistoryService.getLastReadStoriesByUser(userId);

        return ResponseEntity
                .ok(ApiResponse.success("Lấy danh sách story đã đọc gần đây thành công", lastReadStories));
    }
}
