package com.meobeo.truyen.domain.response.subscription;

import lombok.Data;

import java.util.List;

@Data
public class SubscriptionListResponse {

    private List<SubscriptionResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
}
