package com.meobeo.truyen.domain.request.chapter;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChapterLockRequest {

    @NotNull(message = "Giá tiền không được để trống")
    @Min(value = 0, message = "Giá tiền phải >= 0")
    private Integer price;

    private Boolean isVipOnly = false;

    private Boolean isLocked = true;
}
