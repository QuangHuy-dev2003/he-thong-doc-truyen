package com.meobeo.truyen.domain.response.topup;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TopupPaymentResponse {

    private String paymentUrl;
    private String orderId;
    private BigDecimal amount;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private String voucherCode;
    private String packageName;
    private Long packageId;
}
