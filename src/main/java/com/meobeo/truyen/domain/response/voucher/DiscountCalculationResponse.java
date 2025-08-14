package com.meobeo.truyen.domain.response.voucher;

import com.meobeo.truyen.domain.enums.VoucherType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DiscountCalculationResponse {
    private String voucherCode;
    private String voucherName;
    private VoucherType type;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private BigDecimal discountPercentage;
    private boolean isValid;
    private String message;
}
