package com.meobeo.truyen.domain.response.wallet;

import lombok.Data;

import java.util.List;

@Data
public class UserWalletListResponse {

    private List<UserWalletInfoResponse> wallets;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
