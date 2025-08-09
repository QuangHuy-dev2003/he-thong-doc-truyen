package com.meobeo.truyen.controller.chapter;

import com.meobeo.truyen.domain.request.chapter.ChapterLockRequest;
import com.meobeo.truyen.domain.response.chapter.ChapterPaymentResponse;
import com.meobeo.truyen.service.interfaces.ChapterPaymentService;
import com.meobeo.truyen.utils.ApiResponse;
import com.meobeo.truyen.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ChapterPaymentController {

    private final ChapterPaymentService chapterPaymentService;
    private final SecurityUtils securityUtils;

    /**
     * POST /api/v1/chapters/{chapterId}/lock - Khóa chapter và đặt giá tiền
     * Chỉ ADMIN và UPLOADER được phép thực hiện
     */
    @PostMapping("/chapters/{chapterId}/lock")
    @PreAuthorize("hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ChapterPaymentResponse>> lockChapter(
            @PathVariable Long chapterId,
            @Valid @RequestBody ChapterLockRequest request) {

        log.info("API khóa chapter được gọi: chapterId={}, price={}", chapterId, request.getPrice());

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        ChapterPaymentResponse response = chapterPaymentService.lockChapter(chapterId, request, userId);

        return ResponseEntity.ok(ApiResponse.success("Khóa chapter thành công", response));
    }

    /**
     * PUT /api/v1/chapters/{chapterId}/unlock - Mở khóa chapter
     * Chỉ ADMIN và UPLOADER được phép thực hiện
     */
    @PutMapping("/chapters/{chapterId}/unlock")
    @PreAuthorize("hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ChapterPaymentResponse>> unlockChapter(@PathVariable Long chapterId) {

        log.info("API mở khóa chapter được gọi: chapterId={}", chapterId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        ChapterPaymentResponse response = chapterPaymentService.unlockChapter(chapterId, userId);

        return ResponseEntity.ok(ApiResponse.success("Mở khóa chapter thành công", response));
    }

    /**
     * PUT /api/v1/chapters/{chapterId}/payment - Cập nhật thông tin payment
     * Chỉ ADMIN và UPLOADER được phép thực hiện
     */
    @PutMapping("/chapters/{chapterId}/payment")
    @PreAuthorize("hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ChapterPaymentResponse>> updateChapterPayment(
            @PathVariable Long chapterId,
            @Valid @RequestBody ChapterLockRequest request) {

        log.info("API cập nhật payment chapter được gọi: chapterId={}", chapterId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        ChapterPaymentResponse response = chapterPaymentService.updateChapterPayment(chapterId, request, userId);

        return ResponseEntity.ok(ApiResponse.success("Cập nhật payment thành công", response));
    }

    /**
     * GET /api/v1/chapters/{chapterId}/payment - Lấy thông tin payment của chapter
     * Public endpoint để client check trạng thái khóa
     */
    @GetMapping("/chapters/{chapterId}/payment")
    public ResponseEntity<ApiResponse<ChapterPaymentResponse>> getChapterPaymentInfo(@PathVariable Long chapterId) {

        log.info("API lấy thông tin payment chapter được gọi: chapterId={}", chapterId);

        ChapterPaymentResponse response = chapterPaymentService.getChapterPaymentInfo(chapterId);

        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin payment thành công", response));
    }

    /**
     * GET /api/v1/stories/{storyId}/chapter-payments - Lấy danh sách payment của
     * story
     * Chỉ ADMIN và UPLOADER (author của story) được phép xem
     */
    @GetMapping("/stories/{storyId}/chapter-payments")
    @PreAuthorize("hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ChapterPaymentResponse>>> getChapterPaymentsByStory(
            @PathVariable Long storyId) {

        log.info("API lấy danh sách payment theo story được gọi: storyId={}", storyId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        List<ChapterPaymentResponse> responses = chapterPaymentService.getChapterPaymentsByStory(storyId, userId);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách payment thành công", responses));
    }

    /**
     * DELETE /api/v1/chapters/{chapterId}/payment - Xóa payment setting (làm
     * chapter miễn phí)
     * Chỉ ADMIN và UPLOADER được phép thực hiện
     */
    @DeleteMapping("/chapters/{chapterId}/payment")
    @PreAuthorize("hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeChapterPayment(@PathVariable Long chapterId) {

        log.info("API xóa payment setting được gọi: chapterId={}", chapterId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        chapterPaymentService.removeChapterPayment(chapterId, userId);

        return ResponseEntity.ok(ApiResponse.success("Xóa payment setting thành công", null));
    }
}
