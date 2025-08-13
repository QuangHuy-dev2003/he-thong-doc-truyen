package com.meobeo.truyen.domain.response.reading;

import lombok.Data;

import java.util.List;

@Data
public class ReadingHistoryListResponse {

    private List<ReadingHistoryResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
}
