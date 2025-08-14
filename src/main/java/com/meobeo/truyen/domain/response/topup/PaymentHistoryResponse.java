package com.meobeo.truyen.domain.response.topup;

import lombok.Data;

import java.util.List;

@Data
public class PaymentHistoryResponse {

    private List<PaymentTransactionResponse> transactions;
    private int totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
}
