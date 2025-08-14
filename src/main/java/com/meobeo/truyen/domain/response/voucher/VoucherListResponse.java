package com.meobeo.truyen.domain.response.voucher;

import lombok.Data;

import java.util.List;

@Data
public class VoucherListResponse {
    private List<VoucherResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
