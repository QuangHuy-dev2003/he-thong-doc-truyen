package com.meobeo.truyen.domain.response.comment;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class CommentListResponse {

    private List<CommentResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    // Thông tin chapter context
    private Long storyId;
    private Long chapterId;
    private Integer chapterNumber;
    private String chapterTitle;
    private Long totalComments;

    /**
     * Tạo CommentListResponse từ Page<CommentResponse>
     */
    public static CommentListResponse fromPage(Page<CommentResponse> page,
            Long storyId,
            Long chapterId,
            Integer chapterNumber,
            String chapterTitle,
            Long totalComments) {
        CommentListResponse response = new CommentListResponse();
        response.setContent(page.getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());

        // Chapter context info
        response.setStoryId(storyId);
        response.setChapterId(chapterId);
        response.setChapterNumber(chapterNumber);
        response.setChapterTitle(chapterTitle);
        response.setTotalComments(totalComments);

        return response;
    }
}
