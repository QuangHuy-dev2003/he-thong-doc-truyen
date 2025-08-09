package com.meobeo.truyen.controller.chapter;

import com.meobeo.truyen.domain.request.chapter.CreateChapterRequest;
import com.meobeo.truyen.domain.request.chapter.UpdateChapterRequest;
import com.meobeo.truyen.domain.request.comment.CreateCommentRequest;
import com.meobeo.truyen.domain.request.comment.UpdateCommentRequest;
import com.meobeo.truyen.domain.response.chapter.ChapterResponse;
import com.meobeo.truyen.domain.response.chapter.ChapterListResponse;
import com.meobeo.truyen.domain.response.comment.CommentResponse;
import com.meobeo.truyen.domain.response.comment.CommentListResponse;
import com.meobeo.truyen.service.interfaces.ChapterService;
import com.meobeo.truyen.service.interfaces.ChapterCommentService;
import com.meobeo.truyen.utils.ApiResponse;
import com.meobeo.truyen.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ChapterController {

    private final ChapterService chapterService;
    private final ChapterCommentService chapterCommentService;
    private final SecurityUtils securityUtils;

    /**
     * POST /api/v1/stories/{storyId}/chapters - Tạo chapter mới cho story
     * Chỉ ADMIN và UPLOADER được phép tạo chapter
     */
    @PostMapping("/stories/{storyId}/chapters")
    @PreAuthorize("hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ChapterResponse>> createChapter(
            @PathVariable Long storyId,
            @Valid @RequestBody CreateChapterRequest request) {

        log.info("API tạo chapter mới được gọi: storyId={}, chapterNumber={}",
                storyId, request.getChapterNumber());

        // Set storyId từ URL vào request (ưu tiên URL over request body)
        request.setStoryId(storyId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();

        ChapterResponse chapter = chapterService.createChapter(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo chapter thành công", chapter));
    }

    /**
     * PUT /api/v1/stories/{storyId}/chapters/{chapterNumber} - Cập nhật chapter
     * Chỉ ADMIN và UPLOADER (author của story) được phép cập nhật
     */
    @PutMapping("/stories/{storyId}/chapters/{chapterNumber}")
    @PreAuthorize("hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ChapterResponse>> updateChapter(
            @PathVariable Long storyId,
            @PathVariable Integer chapterNumber,
            @Valid @RequestBody UpdateChapterRequest request) {

        log.info("API cập nhật chapter được gọi: storyId={}, chapterNumber={}", storyId, chapterNumber);

        Long userId = securityUtils.getCurrentUserIdOrThrow();

        ChapterResponse chapter = chapterService.updateChapterByStoryAndNumber(storyId, chapterNumber, request, userId);

        return ResponseEntity.ok(ApiResponse.success("Cập nhật chapter thành công", chapter));
    }

    /**
     * DELETE /api/v1/stories/{storyId}/chapters/{chapterNumber} - Xóa chapter
     * Chỉ ADMIN và UPLOADER (author của story) được phép xóa
     */
    @DeleteMapping("/stories/{storyId}/chapters/{chapterNumber}")
    @PreAuthorize("hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteChapter(
            @PathVariable Long storyId,
            @PathVariable Integer chapterNumber) {

        log.info("API xóa chapter được gọi: storyId={}, chapterNumber={}", storyId, chapterNumber);

        Long userId = securityUtils.getCurrentUserIdOrThrow();

        chapterService.deleteChapterByStoryAndNumber(storyId, chapterNumber, userId);

        return ResponseEntity.ok(ApiResponse.success("Xóa chapter thành công", null));
    }

    /**
     * GET /api/v1/stories/{storyId}/chapters/{chapterNumber} - Xem chi tiết chapter
     * theo story và number (Public)
     * Endpoint này public để người dùng có thể đọc chapter
     */
    @GetMapping("/stories/{storyId}/chapters/{chapterNumber}")
    public ResponseEntity<ApiResponse<ChapterResponse>> getChapterDetail(
            @PathVariable Long storyId,
            @PathVariable Integer chapterNumber) {

        log.info("API xem chi tiết chapter được gọi: storyId={}, chapterNumber={}", storyId, chapterNumber);

        Long userId = securityUtils.getCurrentUserId().orElse(null);
        ChapterResponse chapter = chapterService.getChapterDetailByStoryAndNumber(storyId, chapterNumber, userId);

        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chapter thành công", chapter));
    }

    /**
     * GET /api/v1/chapters/{slug} - Xem chi tiết chapter theo slug (Public,
     * fallback)
     * Endpoint backup cho trường hợp cần tìm theo slug globally
     */
    @GetMapping("/chapters/{slug}")
    public ResponseEntity<ApiResponse<ChapterResponse>> getChapterDetailBySlug(
            @PathVariable String slug) {

        log.info("API xem chi tiết chapter theo slug được gọi: slug={}", slug);

        Long userId = securityUtils.getCurrentUserId().orElse(null);
        ChapterResponse chapter = chapterService.getChapterDetail(slug, userId);

        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chapter thành công", chapter));
    }

    /**
     * GET /api/v1/stories/{storyIdentifier}/chapters - Lấy danh sách chapter của
     * truyện (Public)
     * Endpoint này public để hiển thị danh sách chapter cho người đọc
     */
    @GetMapping("/stories/{storyIdentifier}/chapters")
    public ResponseEntity<ApiResponse<ChapterListResponse>> getChaptersByStory(
            @PathVariable String storyIdentifier,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        log.info("API lấy danh sách chapter được gọi: storyIdentifier={}, page={}, size={}",
                storyIdentifier, page, size);

        // Validation page parameters
        validatePageParameters(page, size);

        Pageable pageable = PageRequest.of(page, size);
        ChapterListResponse chapters = chapterService.getChaptersByStory(storyIdentifier, pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chapter thành công", chapters));
    }

    /**
     * GET /api/v1/stories/{storyId}/chapters/{chapterNumber}/next - Lấy chapter
     * tiếp theo (Public)
     */
    @GetMapping("/stories/{storyId}/chapters/{chapterNumber}/next")
    public ResponseEntity<ApiResponse<ChapterResponse>> getNextChapter(
            @PathVariable Long storyId,
            @PathVariable Integer chapterNumber) {

        log.info("API lấy chapter tiếp theo được gọi: storyId={}, chapterNumber={}", storyId, chapterNumber);

        ChapterResponse nextChapter = chapterService.getNextChapterByStoryAndNumber(storyId, chapterNumber);

        if (nextChapter != null) {
            return ResponseEntity.ok(ApiResponse.success("Lấy chapter tiếp theo thành công", nextChapter));
        } else {
            return ResponseEntity.ok(ApiResponse.success("Đã là chapter cuối cùng", null));
        }
    }

    /**
     * GET /api/v1/stories/{storyId}/chapters/{chapterNumber}/previous - Lấy chapter
     * trước đó (Public)
     */
    @GetMapping("/stories/{storyId}/chapters/{chapterNumber}/previous")
    public ResponseEntity<ApiResponse<ChapterResponse>> getPreviousChapter(
            @PathVariable Long storyId,
            @PathVariable Integer chapterNumber) {

        log.info("API lấy chapter trước đó được gọi: storyId={}, chapterNumber={}", storyId, chapterNumber);

        ChapterResponse previousChapter = chapterService.getPreviousChapterByStoryAndNumber(storyId, chapterNumber);

        if (previousChapter != null) {
            return ResponseEntity.ok(ApiResponse.success("Lấy chapter trước đó thành công", previousChapter));
        } else {
            return ResponseEntity.ok(ApiResponse.success("Đây là chapter đầu tiên", null));
        }
    }

    /**
     * GET /api/v1/chapters/check-slug/{slug} - Kiểm tra slug chapter đã tồn tại
     * chưa (Public)
     */
    @GetMapping("/chapters/check-slug/{slug}")
    public ResponseEntity<ApiResponse<Boolean>> checkSlugExists(@PathVariable String slug) {

        log.info("API kiểm tra slug chapter được gọi: slug={}", slug);

        boolean exists = chapterService.isSlugExists(slug);

        return ResponseEntity.ok(ApiResponse.success("Kiểm tra slug thành công", exists));
    }

    /**
     * GET /api/v1/stories/{storyId}/chapters/check-number/{chapterNumber} - Kiểm
     * tra
     * chapter number đã tồn tại chưa trong story
     */
    @GetMapping("/stories/{storyId}/chapters/check-number/{chapterNumber}")
    @PreAuthorize("hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> checkChapterNumberExists(
            @PathVariable Long storyId,
            @PathVariable Integer chapterNumber) {

        log.info("API kiểm tra chapter number được gọi: storyId={}, chapterNumber={}", storyId, chapterNumber);

        boolean exists = chapterService.isChapterNumberExists(storyId, chapterNumber);

        return ResponseEntity.ok(ApiResponse.success("Kiểm tra chapter number thành công", exists));
    }

    /**
     * GET /api/v1/chapters/{chapterId}/can-edit - Kiểm tra quyền chỉnh sửa chapter
     */
    @GetMapping("/chapters/{chapterId}/can-edit")
    @PreAuthorize("hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> canEditChapter(@PathVariable Long chapterId) {

        log.info("API kiểm tra quyền chỉnh sửa chapter được gọi: chapterId={}", chapterId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        boolean canEdit = chapterService.canEditChapter(chapterId, userId);

        return ResponseEntity.ok(ApiResponse.success("Kiểm tra quyền chỉnh sửa thành công", canEdit));
    }

    // ========================== COMMENT APIs ==========================

    /**
     * POST /api/v1/stories/{storyId}/chapters/{chapterNumber}/comments - Tạo
     * comment cho chapter
     * Yêu cầu đăng nhập (USER, UPLOADER, ADMIN)
     */
    @PostMapping("/stories/{storyId}/chapters/{chapterNumber}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable Long storyId,
            @PathVariable Integer chapterNumber,
            @Valid @RequestBody CreateCommentRequest request) {

        log.info("API tạo comment được gọi: storyId={}, chapterNumber={}", storyId, chapterNumber);

        Long userId = securityUtils.getCurrentUserIdOrThrow();

        CommentResponse comment = chapterCommentService.createComment(storyId, chapterNumber, request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo comment thành công", comment));
    }

    /**
     * GET /api/v1/stories/{storyId}/chapters/{chapterNumber}/comments - Lấy danh
     * sách comment của chapter (Public)
     */
    @GetMapping("/stories/{storyId}/chapters/{chapterNumber}/comments")
    public ResponseEntity<ApiResponse<CommentListResponse>> getChapterComments(
            @PathVariable Long storyId,
            @PathVariable Integer chapterNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("API lấy comment chapter được gọi: storyId={}, chapterNumber={}, page={}, size={}",
                storyId, chapterNumber, page, size);

        // Validation page parameters
        validatePageParameters(page, size);

        Pageable pageable = PageRequest.of(page, size);
        CommentListResponse comments = chapterCommentService.getCommentsByStoryAndChapter(storyId, chapterNumber,
                pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách comment thành công", comments));
    }

    /**
     * GET /api/v1/chapters/{chapterId}/comments - Lấy danh sách comment theo
     * chapterId (Public, fallback)
     */
    @GetMapping("/chapters/{chapterId}/comments")
    public ResponseEntity<ApiResponse<CommentListResponse>> getChapterCommentsByChapterId(
            @PathVariable Long chapterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("API lấy comment theo chapterId được gọi: chapterId={}, page={}, size={}", chapterId, page, size);

        // Validation page parameters
        validatePageParameters(page, size);

        Pageable pageable = PageRequest.of(page, size);
        CommentListResponse comments = chapterCommentService.getCommentsByChapter(chapterId, pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách comment thành công", comments));
    }

    /**
     * PUT /api/v1/comments/{commentId} - Cập nhật comment
     * Chỉ ADMIN hoặc người tạo comment được phép sửa
     * Yêu cầu đăng nhập và rate limiting: không quá 3 lần trong 5 phút
     */
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {

        log.info("API cập nhật comment được gọi: commentId={}", commentId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();

        CommentResponse updatedComment = chapterCommentService.updateComment(commentId, request, userId);

        return ResponseEntity.ok(ApiResponse.success("Cập nhật comment thành công", updatedComment));
    }

    /**
     * DELETE /api/v1/comments/{commentId} - Xóa comment
     * Chỉ ADMIN hoặc người tạo comment được phép xóa
     * Yêu cầu đăng nhập (được xử lý bởi
     * SecurityConfig.anyRequest().authenticated())
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long commentId) {

        log.info("API xóa comment được gọi: commentId={}", commentId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();

        chapterCommentService.deleteComment(commentId, userId);

        return ResponseEntity.ok(ApiResponse.success("Xóa comment thành công", null));
    }

    /**
     * GET /api/v1/chapters/{chapterId}/comments/count - Đếm số comment của chapter
     * (Public)
     */
    @GetMapping("/chapters/{chapterId}/comments/count")
    public ResponseEntity<ApiResponse<Long>> countChapterComments(@PathVariable Long chapterId) {

        log.info("API đếm comment chapter được gọi: chapterId={}", chapterId);

        Long totalComments = chapterCommentService.countCommentsByChapter(chapterId);

        return ResponseEntity.ok(ApiResponse.success("Đếm comment thành công", totalComments));
    }

    // Helper methods for validation

    /**
     * Validation cho page parameters
     */
    private void validatePageParameters(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Trang phải >= 0");
        }

        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Kích thước trang phải từ 1-100");
        }
    }
}
