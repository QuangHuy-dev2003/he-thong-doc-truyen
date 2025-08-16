package com.meobeo.truyen.domain.response.recommendation;

import lombok.Data;

import java.util.List;

@Data
public class RecommendationListResponse {

    private List<RecommendationResponse> recommendations;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
