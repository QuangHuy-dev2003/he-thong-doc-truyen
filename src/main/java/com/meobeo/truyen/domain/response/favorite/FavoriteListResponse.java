package com.meobeo.truyen.domain.response.favorite;

import lombok.Data;

import java.util.List;

@Data
public class FavoriteListResponse {

    private List<FavoriteResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
}
