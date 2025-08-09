package com.meobeo.truyen.domain.response.story;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class StoryListResponse {

    private List<StoryResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    public static StoryListResponse fromPage(Page<StoryResponse> page) {
        StoryListResponse response = new StoryListResponse();
        response.setContent(page.getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());
        return response;
    }
}