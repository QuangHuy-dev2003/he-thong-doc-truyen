package com.meobeo.truyen.domain.request.topup;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTopupPackageRequest {

    @NotBlank(message = "Tên gói không được để trống")
    private String name;

    @NotNull(message = "Mệnh giá không được để trống")
    @Min(value = 1, message = "Mệnh giá phải lớn hơn 0")
    private Integer amount;

    @NotNull(message = "Giá tiền không được để trống")
    @Min(value = 1, message = "Giá tiền phải lớn hơn 0")
    private Integer price;

    @Min(value = 0, message = "Phần trăm bonus không được âm")
    private Double bonusPercentage = 0.0;

    private String description;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean isActive;
}
