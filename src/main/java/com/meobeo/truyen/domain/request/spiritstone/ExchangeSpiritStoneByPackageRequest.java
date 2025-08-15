package com.meobeo.truyen.domain.request.spiritstone;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExchangeSpiritStoneByPackageRequest {

    @NotNull(message = "ID gói không được để trống")
    private Long packageId;
}
