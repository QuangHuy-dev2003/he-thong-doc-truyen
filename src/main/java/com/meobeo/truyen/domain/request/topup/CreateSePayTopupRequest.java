package com.meobeo.truyen.domain.request.topup;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateSePayTopupRequest {

    @NotNull(message = "ID gói nạp tiền không được để trống")
    private Long packageId;

    private String voucherCode;
}
