package com.meobeo.truyen.domain.request.giftcode;

import com.meobeo.truyen.domain.enums.GiftCodeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGiftCodeRequest {

    @NotBlank(message = "Tên quà tặng không được để trống")
    private String name;

    @NotNull(message = "Số lượng linh thạch không được để trống")
    @Min(value = 1, message = "Số lượng linh thạch phải lớn hơn 0")
    private Integer amount;

    @NotNull(message = "Loại quà tặng không được để trống")
    private GiftCodeType type;

    @Min(value = 0, message = "Số lượt sử dụng tối đa không được âm")
    private Integer maxUsageCount;

    @Min(value = 0, message = "Số người sử dụng tối đa không được âm")
    private Integer maxUsersCount;

    @Min(value = 1, message = "Số lượt sử dụng tối đa mỗi người phải lớn hơn 0")
    private Integer maxUsagePerUser = 1;

    private String description;

    private Boolean isActive;
}
