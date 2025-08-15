package com.meobeo.truyen.domain.request.spiritstone;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateSpiritStonePackageRequest {

    @NotBlank(message = "Tên gói không được để trống")
    private String name;

    @NotNull(message = "Số linh thạch không được để trống")
    @Min(value = 1, message = "Số linh thạch phải lớn hơn 0")
    private Integer spiritStones;

    @NotNull(message = "Giá tiền không được để trống")
    @Min(value = 1, message = "Giá tiền phải lớn hơn 0")
    private Integer price;

    @Min(value = 0, message = "Phần trăm khuyến mãi không được âm")
    private Double bonusPercentage = 0.0;

    private String description;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean isActive;
}
