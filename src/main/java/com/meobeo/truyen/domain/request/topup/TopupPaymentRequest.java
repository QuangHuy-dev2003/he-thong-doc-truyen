package com.meobeo.truyen.domain.request.topup;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TopupPaymentRequest {

    @NotNull(message = "ID gói nạp tiền không được để trống")
    private Long packageId;

    private String voucherCode;
}
