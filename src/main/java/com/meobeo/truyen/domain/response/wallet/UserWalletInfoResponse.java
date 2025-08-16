package com.meobeo.truyen.domain.response.wallet;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserWalletInfoResponse {

    private Long userId;
    private String username;
    private String displayName;
    private String email;
    private Integer balance; // Số dư tiền mặt (VND)
    private Integer spiritStones; // Số linh thạch
    private Integer recommendationTickets; // Số phiếu đề cử
    private LocalDateTime lastTransactionAt;
    private LocalDateTime createdAt;
}
