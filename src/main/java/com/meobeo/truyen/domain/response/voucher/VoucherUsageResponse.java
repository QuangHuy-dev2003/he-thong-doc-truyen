package com.meobeo.truyen.domain.response.voucher;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherUsageResponse {
    private Long voucherId;
    private String voucherCode;
    private String voucherName;
    private Long userId;
    private String userName;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private LocalDateTime usedAt;
}
