package com.meobeo.truyen.controller.auth;

import com.meobeo.truyen.domain.request.auth.ForgotPasswordDto;
import com.meobeo.truyen.domain.request.auth.LoginRequestDto;
import com.meobeo.truyen.domain.request.auth.OtpVerificationDto;
import com.meobeo.truyen.domain.request.auth.RefreshTokenRequestDto;
import com.meobeo.truyen.domain.request.auth.ResendOtpDto;
import com.meobeo.truyen.domain.request.auth.ResetPasswordDto;
import com.meobeo.truyen.domain.request.auth.UserRegistrationDto;
import com.meobeo.truyen.domain.response.auth.LoginResponseDto;
import com.meobeo.truyen.domain.response.auth.RefreshTokenResponseDto;
import com.meobeo.truyen.domain.response.auth.UserResponseDto;
import com.meobeo.truyen.utils.ApiResponse;
import com.meobeo.truyen.utils.SecurityUtils;
import com.meobeo.truyen.service.interfaces.AuthService;
import com.meobeo.truyen.service.interfaces.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final SecurityUtils securityUtils;

    @PostMapping("/auth/register")
    public ResponseEntity<ApiResponse<UserResponseDto>> registerUser(
            @Valid @RequestBody UserRegistrationDto registrationDto) {
        log.info("Nhận request đăng ký user: {}", registrationDto.getUsername());

        UserResponseDto userResponse = userService.registerUser(registrationDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký tài khoản thành công. Vui lòng kiểm tra email để xác thực OTP.",
                        userResponse));
    }

    @PostMapping("/auth/verify-otp")
    public ResponseEntity<ApiResponse<Boolean>> verifyOtp(
            @Valid @RequestBody OtpVerificationDto otpVerificationDto) {
        log.info("Nhận request xác thực OTP cho email: {}", otpVerificationDto.getEmail());

        boolean isVerified = userService.verifyOtpAndActivateAccount(otpVerificationDto);

        if (isVerified) {
            return ResponseEntity
                    .ok(ApiResponse.success("Xác thực OTP thành công. Tài khoản đã được kích hoạt.", true));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Mã OTP không hợp lệ hoặc đã hết hạn.", false));
        }
    }

    @PostMapping("/auth/resend-otp")
    public ResponseEntity<ApiResponse<String>> resendOtp(
            @Valid @RequestBody ResendOtpDto resendOtpDto) {
        log.info("Nhận request gửi lại OTP cho email: {}", resendOtpDto.getEmail());

        try {
            userService.resendOtp(resendOtpDto);
            return ResponseEntity
                    .ok(ApiResponse.success("Đã gửi lại mã OTP thành công. Vui lòng kiểm tra email.", "OTP sent"));
        } catch (Exception e) {
            log.error("Lỗi gửi lại OTP: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Không thể gửi lại mã OTP. " + e.getMessage(), null));
        }
    }

    @GetMapping("/auth/check-username/{username}")
    public ResponseEntity<ApiResponse<Boolean>> checkUsernameExists(@PathVariable String username) {
        boolean exists = userService.existsByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(exists));
    }

    @GetMapping("/auth/check-email/{email}")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailExists(@PathVariable String email) {
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(exists));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto loginRequest,
            HttpServletResponse response) {
        log.info("Nhận request đăng nhập cho email: {}", loginRequest.getEmail());

        LoginResponseDto loginResponse = authService.login(loginRequest);

        // Tạo HttpOnly cookie cho access token
        Cookie accessTokenCookie = new Cookie("access_token", loginResponse.getAccessToken());
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(true); // Chỉ gửi qua HTTPS
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(900); // 15 phút
        response.addCookie(accessTokenCookie);

        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", loginResponse));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponseDto>> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDto refreshTokenRequest,
            HttpServletResponse response) {
        log.info("Nhận request refresh token");

        RefreshTokenResponseDto refreshResponse = authService.refreshToken(refreshTokenRequest);

        // Tạo HttpOnly cookie cho access token mới
        Cookie accessTokenCookie = new Cookie("access_token", refreshResponse.getAccessToken());
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(900); // 15 phút
        response.addCookie(accessTokenCookie);

        return ResponseEntity.ok(ApiResponse.success("Refresh token thành công", refreshResponse));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse<String>> logout(Authentication authentication, HttpServletResponse response) {
        if (authentication != null && authentication.isAuthenticated()) {
            // Lấy userId từ authentication
            securityUtils.getCurrentUserId().ifPresent(userId -> {
                authService.logout(userId);
            });

            // Xóa cookie
            Cookie accessTokenCookie = new Cookie("access_token", null);
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setSecure(true);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setMaxAge(0);
            response.addCookie(accessTokenCookie);
        }

        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", "Logged out"));
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordDto forgotPasswordDto) {
        log.info("Nhận request quên mật khẩu cho email: {}", forgotPasswordDto.getEmail());

        try {
            userService.sendForgotPasswordOtp(forgotPasswordDto);
            return ResponseEntity
                    .ok(ApiResponse.success("Đã gửi mã OTP đặt lại mật khẩu. Vui lòng kiểm tra email.", "OTP sent"));
        } catch (Exception e) {
            log.error("Lỗi gửi OTP quên mật khẩu: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Không thể gửi mã OTP. " + e.getMessage(), null));
        }
    }

    @PostMapping("/auth/reset-password")
    public ResponseEntity<ApiResponse<Boolean>> resetPassword(
            @Valid @RequestBody ResetPasswordDto resetPasswordDto) {
        log.info("Nhận request đặt lại mật khẩu cho email: {}", resetPasswordDto.getEmail());

        boolean isReset = userService.resetPassword(resetPasswordDto);

        if (isReset) {
            return ResponseEntity
                    .ok(ApiResponse.success("Đặt lại mật khẩu thành công.", true));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Mã OTP không hợp lệ hoặc đã hết hạn.", false));
        }
    }
}