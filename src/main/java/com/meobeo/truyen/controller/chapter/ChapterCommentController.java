package com.meobeo.truyen.controller.chapter;

import com.meobeo.truyen.domain.request.comment.CreateCommentRequest;
import com.meobeo.truyen.domain.request.comment.UpdateCommentRequest;
import com.meobeo.truyen.domain.response.comment.CommentResponse;
import com.meobeo.truyen.domain.response.comment.CommentListResponse;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ChapterCommentController {

    private final ChapterCommentService chapterCommentService;
    private final SecurityUtils securityUtils;

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
