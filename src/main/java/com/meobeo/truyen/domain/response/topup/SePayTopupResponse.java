package com.meobeo.truyen.domain.response.topup;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SePayTopupResponse {

    private Long requestId;
    private BigDecimal amount;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private String voucherCode;
    private String packageName;
    private Long packageId;
    private String qrUrl;
    private String transferContent;
    private String accountNumber;
    private String bankName;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
