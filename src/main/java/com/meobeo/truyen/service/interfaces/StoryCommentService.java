package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.comment.CreateCommentRequest;
import com.meobeo.truyen.domain.request.comment.UpdateCommentRequest;
import com.meobeo.truyen.domain.response.comment.CommentResponse;
import com.meobeo.truyen.domain.response.comment.CommentListResponse;
import org.springframework.data.domain.Pageable;

public interface StoryCommentService {

    /**
     * Tạo comment mới cho story
     */
    CommentResponse createComment(Long storyId, CreateCommentRequest request, Long userId);

    /**
     * Lấy danh sách comment của story với phân trang
     */
    CommentListResponse getCommentsByStory(Long storyId, Pageable pageable);

    /**
     * Cập nhật comment (chỉ admin hoặc chính người tạo comment)
     */
    CommentResponse updateComment(Long commentId, UpdateCommentRequest request, Long userId);

    /**
     * Xóa comment (chỉ admin hoặc chính người tạo comment)
     */
    void deleteComment(Long commentId, Long userId);

    /**
     * Kiểm tra user có thể comment không (rate limiting)
     */
    boolean canUserComment(Long userId, Long storyId);

    /**
     * Đếm tổng số comment của story
     */
    Long countCommentsByStory(Long storyId);
}
