package com.meobeo.truyen.domain.response.spiritstone;

import lombok.Data;

@Data
public class WalletBalanceResponse {

    private Integer balance; // Số dư tiền mặt (VND)
    private Integer spiritStones; // Số linh thạch
    private Integer amountSpent; // Số tiền đã chi
    private Integer spiritStonesReceived; // Số linh thạch đã nhận
    private Integer recommendationTicketsEarned; // Số phiếu đề cử được tặng
    private String description; // Mô tả giao dịch
}
