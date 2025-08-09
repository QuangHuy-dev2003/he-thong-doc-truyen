package com.meobeo.truyen.domain.response.chapter;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class ChapterListResponse {

    private List<ChapterResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    // Thông tin truyện
    private Long storyId;
    private String storyTitle;
    private String storySlug;

    public static ChapterListResponse fromPage(Page<ChapterResponse> page) {
        ChapterListResponse response = new ChapterListResponse();
        response.setContent(page.getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());

        // Lấy thông tin story từ chapter đầu tiên nếu có
        if (!page.getContent().isEmpty()) {
            ChapterResponse firstChapter = page.getContent().get(0);
            response.setStoryId(firstChapter.getStoryId());
            response.setStoryTitle(firstChapter.getStoryTitle());
            response.setStorySlug(firstChapter.getStorySlug());
        }

        return response;
    }
}
