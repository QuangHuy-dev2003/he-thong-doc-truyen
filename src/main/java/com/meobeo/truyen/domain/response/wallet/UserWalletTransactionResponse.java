package com.meobeo.truyen.domain.response.wallet;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserWalletTransactionResponse {

    private Long id;
    private Long userId;
    private String username;
    private String displayName;
    private Integer amount;
    private String currency;
    private String type;
    private String description;
    private LocalDateTime createdAt;
}
