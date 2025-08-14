package com.meobeo.truyen.domain.request.voucher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApplyVoucherRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    private String voucherCode;

    @NotNull(message = "Số tiền nạp không được để trống")
    @Positive(message = "Số tiền nạp phải lớn hơn 0")
    private BigDecimal amount;
}
