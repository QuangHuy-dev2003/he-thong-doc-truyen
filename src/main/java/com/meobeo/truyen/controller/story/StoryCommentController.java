package com.meobeo.truyen.controller.story;

import com.meobeo.truyen.domain.request.comment.CreateCommentRequest;
import com.meobeo.truyen.domain.request.comment.UpdateCommentRequest;
import com.meobeo.truyen.domain.response.comment.CommentResponse;
import com.meobeo.truyen.domain.response.comment.CommentListResponse;
import com.meobeo.truyen.exception.BadRequestException;
import com.meobeo.truyen.service.interfaces.StoryCommentService;
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
public class StoryCommentController {

    private final StoryCommentService storyCommentService;
    private final SecurityUtils securityUtils;

    /**
     * POST /api/v1/stories/{storyId}/comments - Tạo comment cho story
     * Yêu cầu đăng nhập (USER, UPLOADER, ADMIN)
     */
    @PostMapping("/stories/{storyId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable Long storyId,
            @Valid @RequestBody CreateCommentRequest request) {

        log.info("API tạo story comment được gọi: storyId={}", storyId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();

        CommentResponse comment = storyCommentService.createComment(storyId, request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo comment thành công", comment));
    }

    /**
     * GET /api/v1/stories/{storyId}/comments - Lấy danh sách comment của story
     * (Public)
     */
    @GetMapping("/stories/{storyId}/comments")
    public ResponseEntity<ApiResponse<CommentListResponse>> getStoryComments(
            @PathVariable Long storyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("API lấy comment story được gọi: storyId={}, page={}, size={}",
                storyId, page, size);

        // Validation page parameters
        validatePageParameters(page, size);

        Pageable pageable = PageRequest.of(page, size);
        CommentListResponse comments = storyCommentService.getCommentsByStory(storyId, pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách comment thành công", comments));
    }

    /**
     * PUT /api/v1/story-comments/{commentId} - Cập nhật story comment
     * Chỉ ADMIN hoặc người tạo comment được phép sửa
     * Yêu cầu đăng nhập và rate limiting: không quá 3 lần trong 5 phút
     */
    @PutMapping("/story-comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {

        log.info("API cập nhật story comment được gọi: commentId={}", commentId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();

        CommentResponse updatedComment = storyCommentService.updateComment(commentId, request, userId);

        return ResponseEntity.ok(ApiResponse.success("Cập nhật comment thành công", updatedComment));
    }

    /**
     * DELETE /api/v1/story-comments/{commentId} - Xóa story comment
     * Chỉ ADMIN hoặc người tạo comment được phép xóa
     * Yêu cầu đăng nhập (được xử lý bởi
     * SecurityConfig.anyRequest().authenticated())
     */
    @DeleteMapping("/story-comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long commentId) {

        log.info("API xóa story comment được gọi: commentId={}", commentId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();

        storyCommentService.deleteComment(commentId, userId);

        return ResponseEntity.ok(ApiResponse.success("Xóa comment thành công", null));
    }

    /**
     * GET /api/v1/stories/{storyId}/comments/count - Đếm số comment của story
     * (Public)
     */
    @GetMapping("/stories/{storyId}/comments/count")
    public ResponseEntity<ApiResponse<Long>> countStoryComments(@PathVariable Long storyId) {

        log.info("API đếm comment story được gọi: storyId={}", storyId);

        Long totalComments = storyCommentService.countCommentsByStory(storyId);

        return ResponseEntity.ok(ApiResponse.success("Đếm comment thành công", totalComments));
    }

    // Helper methods for validation

    /**
     * Validation cho page parameters
     */
    private void validatePageParameters(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Trang phải >= 0");
        }

        if (size <= 0 || size > 100) {
            throw new BadRequestException("Kích thước trang phải từ 1-100");
        }
    }
}
