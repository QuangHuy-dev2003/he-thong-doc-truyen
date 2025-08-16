package com.meobeo.truyen.domain.response.recommendation;

import com.meobeo.truyen.domain.response.story.StoryResponse;
import lombok.Data;

import java.util.List;

@Data
public class TopRecommendedStoriesResponse {

    private List<StoryResponse> stories;
    private List<Long> recommendationCounts;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
