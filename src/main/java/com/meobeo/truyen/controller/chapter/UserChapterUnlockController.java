package com.meobeo.truyen.controller.chapter;

import com.meobeo.truyen.domain.request.chapter.UnlockChapterRequest;
import com.meobeo.truyen.domain.request.chapter.UnlockChapterRangeRequest;
import com.meobeo.truyen.domain.request.chapter.UnlockFullStoryRequest;
import com.meobeo.truyen.domain.response.chapter.ChapterLockStatusResponse;
import com.meobeo.truyen.domain.response.chapter.UnlockChapterBatchResponse;
import com.meobeo.truyen.domain.response.chapter.UnlockChapterResponse;
import com.meobeo.truyen.domain.response.chapter.UnlockFullStoryResponse;
import com.meobeo.truyen.domain.response.chapter.UnlockedChaptersResponse;
import com.meobeo.truyen.mapper.ChapterUnlockMapper;
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
     * Mở khóa 1 chương
     */
    @PostMapping("/chapters/{chapterId}/unlock")
    public ResponseEntity<ApiResponse<UnlockChapterResponse>> unlockChapter(
            @PathVariable Long chapterId,
            @Valid @RequestBody UnlockChapterRequest request,
            Authentication authentication) {

        log.info("User {} yêu cầu mở khóa chương {}", authentication.getName(), chapterId);

        // Đảm bảo chapterId trong path và request giống nhau
        request.setChapterId(chapterId);

        Long userId = Long.parseLong(authentication.getName());
        UnlockChapterResponse response = chapterUnlockService.unlockChapter(request, userId);

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

        log.info("User {} yêu cầu mở khóa chương từ {} đến {} cho story {}",
                authentication.getName(), request.getFromChapterNumber(), request.getToChapterNumber(), storyId);

        // Đảm bảo storyId trong path và request giống nhau
        request.setStoryId(storyId);

        Long userId = Long.parseLong(authentication.getName());
        UnlockChapterBatchResponse response = chapterUnlockService.unlockChapterRange(request, userId);

        return ResponseEntity.ok(ApiResponse.success("Mở khóa chương thành công", response));
    }

    /**
     * Mở khóa full truyện
     */
    @PostMapping("/stories/{storyId}/unlock-full")
    public ResponseEntity<ApiResponse<UnlockFullStoryResponse>> unlockFullStory(
            @PathVariable Long storyId,
            @Valid @RequestBody UnlockFullStoryRequest request,
            Authentication authentication) {

        log.info("User {} yêu cầu mở khóa full truyện {}", authentication.getName(), storyId);

        // Đảm bảo storyId trong path và request giống nhau
        request.setStoryId(storyId);

        Long userId = Long.parseLong(authentication.getName());
        UnlockFullStoryResponse response = chapterUnlockService.unlockFullStory(request, userId);

        return ResponseEntity.ok(ApiResponse.success("Mở khóa full truyện thành công", response));
    }

    /**
     * Kiểm tra trạng thái khóa chương
     */
    @GetMapping("/chapters/{chapterId}/lock-status")
    public ResponseEntity<ApiResponse<ChapterLockStatusResponse>> getChapterLockStatus(
            @PathVariable Long chapterId,
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());
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

        Long userId = Long.parseLong(authentication.getName());

        // Tạo Pageable
        Pageable pageable = PageRequest.of(page, size);

        // Lấy danh sách có phân trang
        var unlockedChaptersPage = chapterUnlockService.getUserUnlockedChapterIds(userId, storyId, pageable);

        // Tạo response với mapper
        UnlockedChaptersResponse response = chapterUnlockMapper.toUnlockedChaptersResponse(
                unlockedChaptersPage, storyId, "Story Title");

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chương đã mở khóa thành công", response));
    }
}
