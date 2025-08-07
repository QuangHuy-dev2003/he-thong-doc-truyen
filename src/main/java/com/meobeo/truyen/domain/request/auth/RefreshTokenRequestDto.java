package com.meobeo.truyen.domain.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequestDto {

    @NotBlank(message = "Refresh token không được để trống")
    private String refreshToken;
}