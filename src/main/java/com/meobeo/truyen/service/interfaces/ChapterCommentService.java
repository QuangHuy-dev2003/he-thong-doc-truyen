package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.comment.CreateCommentRequest;
import com.meobeo.truyen.domain.response.comment.CommentResponse;
import com.meobeo.truyen.domain.response.comment.CommentListResponse;
import org.springframework.data.domain.Pageable;

public interface ChapterCommentService {

    /**
     * Tạo comment mới cho chapter
     */
    CommentResponse createComment(Long storyId, Integer chapterNumber, CreateCommentRequest request, Long userId);

    /**
     * Lấy danh sách comment của chapter theo storyId và chapterNumber
     */
    CommentListResponse getCommentsByStoryAndChapter(Long storyId, Integer chapterNumber, Pageable pageable);

    /**
     * Lấy danh sách comment của chapter theo chapterId
     */
    CommentListResponse getCommentsByChapter(Long chapterId, Pageable pageable);

    /**
     * Xóa comment (chỉ admin hoặc chính người tạo comment)
     */
    void deleteComment(Long commentId, Long userId);

    /**
     * Kiểm tra user có thể comment không (rate limiting)
     */
    boolean canUserComment(Long userId, Long chapterId);

    /**
     * Đếm tổng số comment của chapter
     */
    Long countCommentsByChapter(Long chapterId);
}
