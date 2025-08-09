package com.meobeo.truyen.domain.response.chapter;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class ChapterListResponse {

    private List<ChapterSummaryDto> content;
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

    public static ChapterListResponse fromPage(Page<ChapterSummaryDto> page) {
        ChapterListResponse response = new ChapterListResponse();
        response.setContent(page.getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());

        return response;
    }

    public static ChapterListResponse fromPageWithStoryInfo(Page<ChapterSummaryDto> page,
            Long storyId, String storyTitle, String storySlug) {
        ChapterListResponse response = fromPage(page);
        response.setStoryId(storyId);
        response.setStoryTitle(storyTitle);
        response.setStorySlug(storySlug);
        return response;
    }
}
