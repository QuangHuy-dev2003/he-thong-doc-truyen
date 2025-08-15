package com.meobeo.truyen.domain.response.spiritstone;

import lombok.Data;

import java.util.List;

@Data
public class ExchangeHistoryListResponse {

    private List<ExchangeHistoryResponse> exchanges;
    private long totalCount;
    private int totalPages;
    private int currentPage;
    private int pageSize;
}
