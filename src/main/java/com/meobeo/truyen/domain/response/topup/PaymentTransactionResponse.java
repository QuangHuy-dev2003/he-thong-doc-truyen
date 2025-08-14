package com.meobeo.truyen.domain.response.topup;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentTransactionResponse {

    private Long id;
    private String orderId;
    private String vnpayTransactionId;
    private BigDecimal amount;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private String voucherCode;
    private String status;
    private String paymentUrl;
    private String vnpayResponseCode;
    private String vnpayResponseMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime paidAt;

    // Thông tin gói nạp tiền
    private Long packageId;
    private String packageName;

    // Thông tin user
    private Long userId;
    private String username;
}
