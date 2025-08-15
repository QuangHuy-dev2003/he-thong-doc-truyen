package com.meobeo.truyen.domain.request.spiritstone;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExchangeSpiritStoneByAmountRequest {

    @NotNull(message = "Số tiền không được để trống")
    @Min(value = 1, message = "Số tiền phải lớn hơn 0")
    private Integer amount;
}
