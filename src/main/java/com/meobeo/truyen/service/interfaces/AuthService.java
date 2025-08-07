package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.auth.LoginRequestDto;
import com.meobeo.truyen.domain.request.auth.RefreshTokenRequestDto;
import com.meobeo.truyen.domain.response.auth.LoginResponseDto;
import com.meobeo.truyen.domain.response.auth.RefreshTokenResponseDto;

public interface AuthService {

    /**
     * Đăng nhập user
     */
    LoginResponseDto login(LoginRequestDto loginRequest);

    /**
     * Refresh access token
     */
    RefreshTokenResponseDto refreshToken(RefreshTokenRequestDto refreshTokenRequest);

    /**
     * Đăng xuất user
     */
    void logout(Long userId);

    /**
     * Đăng nhập bằng Google OAuth2 (redirect flow)
     */
    LoginResponseDto loginWithGoogleUserInfo(String email, String displayName, String avatarUrl);
}