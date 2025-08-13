package com.meobeo.truyen.domain.request.giftcode;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UseGiftCodeRequest {

    @NotBlank(message = "Mã quà tặng không được để trống")
    private String code;
}
