package com.meobeo.truyen.domain.request.topup;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TopupRequest {

    @NotNull(message = "ID gói cước không được để trống")
    private Long packageId;

    private String voucherCode; // Mã voucher (tùy chọn)
}
