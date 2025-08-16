package com.meobeo.truyen.controller.chapter;

import com.meobeo.truyen.domain.request.chapter.UnlockChapterRangeRequest;
import com.meobeo.truyen.domain.request.chapter.UnlockFullStoryRequest;
import com.meobeo.truyen.domain.response.chapter.ChapterLockStatusResponse;
import com.meobeo.truyen.domain.response.chapter.UnlockChapterBatchResponse;
import com.meobeo.truyen.domain.response.chapter.UnlockChapterResponse;
import com.meobeo.truyen.domain.response.chapter.UnlockFullStoryResponse;
import com.meobeo.truyen.domain.response.chapter.UnlockedChaptersResponse;
import com.meobeo.truyen.mapper.ChapterUnlockMapper;
import com.meobeo.truyen.security.CustomUserDetails;
import com.meobeo.truyen.service.interfaces.ChapterUnlockService;
import com.meobeo.truyen.utils.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class UserChapterUnlockController {

    private final ChapterUnlockService chapterUnlockService;
    private final ChapterUnlockMapper chapterUnlockMapper;

    /**
     * Helper method để lấy userId từ authentication
     */
    private Long getUserIdFromAuthentication(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUserId();
    }

    /**
     * Helper method để lấy user details từ authentication
     */
    private CustomUserDetails getUserDetailsFromAuthentication(Authentication authentication) {
        return (CustomUserDetails) authentication.getPrincipal();
    }

    /**
     * Mở khóa 1 chương
     */
    @PostMapping("/chapters/{chapterId}/unlock")
    public ResponseEntity<ApiResponse<UnlockChapterResponse>> unlockChapter(
            @PathVariable Long chapterId,
            Authentication authentication) {

        CustomUserDetails userDetails = getUserDetailsFromAuthentication(authentication);
        Long userId = userDetails.getUserId();

        log.info("User {} yêu cầu mở khóa chương {}", userDetails.getDisplayName(), chapterId);

        UnlockChapterResponse response = chapterUnlockService.unlockChapter(chapterId, userId);

        return ResponseEntity.ok(ApiResponse.success("Mở khóa chương thành công", response));
    }

    /**
     * Mở khóa từ chương A đến chương B
     */
    @PostMapping("/stories/{storyId}/unlock-range")
    public ResponseEntity<ApiResponse<UnlockChapterBatchResponse>> unlockChapterRange(
            @PathVariable Long storyId,
            @Valid @RequestBody UnlockChapterRangeRequest request,
            Authentication authentication) {

        CustomUserDetails userDetails = getUserDetailsFromAuthentication(authentication);
        Long userId = userDetails.getUserId();

        log.info("User {} yêu cầu mở khóa chương từ {} đến {} cho story {}",
                userDetails.getDisplayName(), request.getFromChapterNumber(), request.getToChapterNumber(), storyId);

        // Kiểm tra số lượng chapter để quyết định sync hay async
        int rangeSize = request.getToChapterNumber() - request.getFromChapterNumber() + 1;

        if (rangeSize >= 20) {
            // Sử dụng async cho range lớn
            String jobId = chapterUnlockService.startAsyncUnlockRange(storyId, request, userId);

            UnlockChapterBatchResponse response = new UnlockChapterBatchResponse();
            response.setJobId(jobId);
            response.setStatus("PENDING");
            response.setMessage("Đã nhận yêu cầu mở khóa. Đang xử lý bất đồng bộ...");

            return ResponseEntity.ok(ApiResponse.success("Đã bắt đầu mở khóa chương bất đồng bộ", response));
        } else {
            // Sử dụng sync cho range nhỏ
            UnlockChapterBatchResponse response = chapterUnlockService.unlockChapterRange(storyId, request, userId);
            return ResponseEntity.ok(ApiResponse.success("Mở khóa chương thành công", response));
        }
    }

    /**
     * Check trạng thái unlock full truyện trước khi mở khóa
     */
    @GetMapping("/stories/{storyId}/unlock-full/check")
    public ResponseEntity<ApiResponse<UnlockFullStoryResponse>> checkUnlockFullStoryStatus(
            @PathVariable Long storyId,
            Authentication authentication) {

        CustomUserDetails userDetails = getUserDetailsFromAuthentication(authentication);
        Long userId = userDetails.getUserId();

        log.info("User {} check trạng thái unlock full truyện {}", userDetails.getDisplayName(), storyId);

        // Tạo empty request object
        UnlockFullStoryRequest request = new UnlockFullStoryRequest();

        // Check trạng thái unlock
        UnlockFullStoryResponse response = chapterUnlockService.checkUnlockFullStoryStatus(storyId, userId);

        return ResponseEntity.ok(ApiResponse.success("Check trạng thái unlock full truyện thành công", response));
    }

    /**
     * Mở khóa full truyện
     */
    @PostMapping("/stories/{storyId}/unlock-full")
    public ResponseEntity<ApiResponse<UnlockFullStoryResponse>> unlockFullStory(
            @PathVariable Long storyId,
            Authentication authentication) {

        CustomUserDetails userDetails = getUserDetailsFromAuthentication(authentication);
        Long userId = userDetails.getUserId();

        log.info("User {} yêu cầu mở khóa full truyện {}", userDetails.getDisplayName(), storyId);

        // Tạo empty request object
        UnlockFullStoryRequest request = new UnlockFullStoryRequest();

        // Luôn sử dụng async cho unlock full story vì thường có nhiều chapter
        String jobId = chapterUnlockService.startAsyncUnlockFullStory(storyId, request, userId);

        UnlockFullStoryResponse response = new UnlockFullStoryResponse();
        response.setJobId(jobId);
        response.setStatus("PENDING");
        response.setMessage("Đã nhận yêu cầu mở khóa full truyện. Đang xử lý bất đồng bộ...");

        return ResponseEntity.ok(ApiResponse.success("Đã bắt đầu mở khóa full truyện bất đồng bộ", response));
    }

    /**
     * Kiểm tra trạng thái khóa chương
     */
    @GetMapping("/chapters/{chapterId}/lock-status")
    public ResponseEntity<ApiResponse<ChapterLockStatusResponse>> getChapterLockStatus(
            @PathVariable Long chapterId,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        ChapterLockStatusResponse response = chapterUnlockMapper.toChapterLockStatusResponse(chapterId, userId);

        return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái khóa chương thành công", response));
    }

    /**
     * Lấy danh sách chương đã mở khóa (có phân trang)
     */
    @GetMapping("/stories/{storyId}/unlocked-chapters")
    public ResponseEntity<ApiResponse<UnlockedChaptersResponse>> getUnlockedChapters(
            @PathVariable Long storyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);

        // Tạo Pageable
        Pageable pageable = PageRequest.of(page, size);

        // Lấy danh sách có phân trang
        var unlockedChaptersPage = chapterUnlockService.getUserUnlockedChapterIds(userId, storyId, pageable);

        // Tạo response với mapper
        UnlockedChaptersResponse response = chapterUnlockMapper.toUnlockedChaptersResponse(
                unlockedChaptersPage, storyId, "Story Title");

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chương đã mở khóa thành công", response));
    }

    /**
     * Kiểm tra trạng thái job unlock range
     */
    @GetMapping("/unlock-range/{jobId}/status")
    public ResponseEntity<ApiResponse<UnlockChapterBatchResponse>> getUnlockRangeStatus(
            @PathVariable String jobId,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);

        var status = chapterUnlockService.getAsyncUnlockRangeStatus(jobId);

        if (status.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái job thành công", status.get()));
        } else {
            return ResponseEntity.ok(ApiResponse.error("Không tìm thấy job với ID: " + jobId));
        }
    }

    /**
     * Kiểm tra trạng thái job unlock full story
     */
    @GetMapping("/unlock-full-story/{jobId}/status")
    public ResponseEntity<ApiResponse<UnlockFullStoryResponse>> getUnlockFullStoryStatus(
            @PathVariable String jobId,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);

        var status = chapterUnlockService.getAsyncUnlockFullStoryStatus(jobId);

        if (status.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái job thành công", status.get()));
        } else {
            return ResponseEntity.ok(ApiResponse.error("Không tìm thấy job với ID: " + jobId));
        }
    }

    /**
     * Hủy job unlock range
     */
    @DeleteMapping("/unlock-range/{jobId}")
    public ResponseEntity<ApiResponse<String>> cancelUnlockRange(
            @PathVariable String jobId,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);

        boolean cancelled = chapterUnlockService.cancelAsyncUnlockRange(jobId, userId);

        if (cancelled) {
            return ResponseEntity.ok(ApiResponse.success("Đã hủy job unlock range thành công", "Job đã được hủy"));
        } else {
            return ResponseEntity.ok(ApiResponse.error("Không thể hủy job hoặc job không tồn tại"));
        }
    }

    /**
     * Hủy job unlock full story
     */
    @DeleteMapping("/unlock-full-story/{jobId}")
    public ResponseEntity<ApiResponse<String>> cancelUnlockFullStory(
            @PathVariable String jobId,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);

        boolean cancelled = chapterUnlockService.cancelAsyncUnlockFullStory(jobId, userId);

        if (cancelled) {
            return ResponseEntity.ok(ApiResponse.success("Đã hủy job unlock full story thành công", "Job đã được hủy"));
        } else {
            return ResponseEntity.ok(ApiResponse.error("Không thể hủy job hoặc job không tồn tại"));
        }
    }
}
