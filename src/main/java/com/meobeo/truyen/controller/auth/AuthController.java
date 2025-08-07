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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import java.lang.reflect.Type;
import java.time.LocalDateTime;

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

    @GetMapping("/auth/oauth2/google-url")
    public ResponseEntity<ApiResponse<String>> getGoogleOAuth2Url() {
        String clientId = System.getenv("GOOGLE_CLIENT_ID");
        String redirectUri = URLEncoder.encode("http://localhost:8080/api/v1/oauth2/callback/google",
                StandardCharsets.UTF_8);
        String scope = URLEncoder.encode("openid email profile", StandardCharsets.UTF_8);
        String state = "meobeo"; // Có thể random hoặc truyền từ frontend nếu muốn bảo mật hơn
        String url = "https://accounts.google.com/o/oauth2/v2/auth?"
                + "client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=" + scope
                + "&state=" + state
                + "&access_type=offline";
        return ResponseEntity.ok(ApiResponse.success("Google OAuth2 URL", url));
    }

    @GetMapping("/oauth2/callback/google")
    public void handleGoogleOAuth2Callback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        if (error != null) {
            // Xử lý lỗi từ Google
            response.sendRedirect("http://localhost:3000/oauth2/error?error=" + error);
            return;
        }

        try {
            // 1. Đổi code lấy access_token từ Google
            String clientId = System.getenv("GOOGLE_CLIENT_ID");
            String clientSecret = System.getenv("GOOGLE_CLIENT_SECRET");
            String redirectUri = "http://localhost:8080/api/v1/oauth2/callback/google";

            String tokenRequestBody = "code=" + code
                    + "&client_id=" + clientId
                    + "&client_secret=" + clientSecret
                    + "&redirect_uri=" + redirectUri
                    + "&grant_type=authorization_code";

            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest tokenRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(tokenRequestBody))
                    .build();

            HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

            if (tokenResponse.statusCode() != 200) {
                response.sendRedirect("http://localhost:3000/oauth2/error?error=token_exchange_failed");
                return;
            }

            // 2. Parse response lấy access_token và id_token
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                        @Override
                        public JsonElement serialize(LocalDateTime src, Type typeOfSrc,
                                JsonSerializationContext context) {
                            return new JsonPrimitive(src.toString());
                        }
                    })
                    .create();
            JsonObject tokenJson = gson.fromJson(tokenResponse.body(), JsonObject.class);
            String accessToken = tokenJson.get("access_token").getAsString();
            String idToken = tokenJson.get("id_token").getAsString();

            // 3. Lấy thông tin user từ Google
            HttpRequest userInfoRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/oauth2/v2/userinfo"))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> userInfoResponse = httpClient.send(userInfoRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (userInfoResponse.statusCode() != 200) {
                response.sendRedirect("http://localhost:3000/oauth2/error?error=userinfo_failed");
                return;
            }

            // 4. Parse thông tin user
            JsonObject userInfo = gson.fromJson(userInfoResponse.body(), JsonObject.class);
            String email = userInfo.get("email").getAsString();
            String displayName = userInfo.has("name") ? userInfo.get("name").getAsString() : email;
            String avatarUrl = userInfo.has("picture") ? userInfo.get("picture").getAsString() : null;

            // 5. Xử lý đăng nhập (tương tự logic cũ)
            LoginResponseDto loginResponse = authService.loginWithGoogleUserInfo(email, displayName, avatarUrl);

            // 6. Redirect về frontend với token
            String frontendUrl = "http://localhost:3000/oauth2/success"
                    + "?access_token=" + loginResponse.getAccessToken()
                    + "&refresh_token=" + loginResponse.getRefreshToken()
                    + "&user=" + URLEncoder.encode(gson.toJson(loginResponse.getUser()), StandardCharsets.UTF_8);

            response.sendRedirect(frontendUrl);

        } catch (Exception e) {
            log.error("Lỗi xử lý Google OAuth2 callback: {}", e.getMessage());
            response.sendRedirect("http://localhost:3000/oauth2/error?error=callback_failed&message=" +
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }
}