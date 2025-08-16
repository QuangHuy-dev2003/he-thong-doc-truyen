package com.meobeo.truyen.domain.response.wallet;

import lombok.Data;

import java.util.List;

@Data
public class UserWalletTransactionListResponse {

    private List<UserWalletTransactionResponse> transactions;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
