package com.meobeo.truyen.domain.request.topup;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SePayWebhookRequest {

    private String gateway;
    private String transactionDate;
    private String accountNumber;
    private String subAccount;
    private String code;
    private String content;
    private String transferType;
    private String description;
    private BigDecimal transferAmount;
    private String referenceCode;
    private BigDecimal accumulated;
    private Long id;
}
