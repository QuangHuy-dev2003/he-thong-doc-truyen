package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.config.JwtConfig;
import com.meobeo.truyen.domain.entity.RefreshToken;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.request.auth.LoginRequestDto;
import com.meobeo.truyen.domain.request.auth.RefreshTokenRequestDto;
import com.meobeo.truyen.domain.response.auth.LoginResponseDto;
import com.meobeo.truyen.domain.response.auth.RefreshTokenResponseDto;
import com.meobeo.truyen.domain.response.auth.UserResponseDto;
import com.meobeo.truyen.exception.InvalidCredentialsException;
import com.meobeo.truyen.exception.InvalidRefreshTokenException;
import com.meobeo.truyen.mapper.UserMapper;
import com.meobeo.truyen.repository.RefreshTokenRepository;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.service.interfaces.AuthService;
import com.meobeo.truyen.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.security.SecureRandom;
import com.meobeo.truyen.domain.enums.AuthProvider;
import com.meobeo.truyen.exception.GoogleOAuth2Exception;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtConfig jwtConfig;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public LoginResponseDto login(LoginRequestDto loginRequest) {
        log.info("Xử lý đăng nhập cho email: {}", loginRequest.getEmail());

        // Tìm user theo email
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Email hoặc password không đúng"));

        // Kiểm tra password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            log.warn("Password không đúng cho email: {}", loginRequest.getEmail());
            throw new InvalidCredentialsException("Email hoặc password không đúng");
        }

        // Kiểm tra tài khoản đã được kích hoạt chưa
        if (!user.getIsActive()) {
            log.warn("Tài khoản chưa kích hoạt cho email: {}", loginRequest.getEmail());
            throw new InvalidCredentialsException("Tài khoản chưa được kích hoạt. Vui lòng xác thực email trước.");
        }

        // Lấy role đầu tiên của user
        String role = user.getRoles().stream()
                .findFirst()
                .map(roleEntity -> roleEntity.getName())
                .orElse("USER");

        log.info("Đăng nhập thành công cho user: {} với role: {}", user.getUsername(), role);

        // Tạo access token và refresh token
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), role);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), role);

        // Lưu refresh token vào database
        saveRefreshToken(user.getId(), refreshToken);

        // Map user thành response DTO
        UserResponseDto userResponse = userMapper.toUserResponseDto(user);

        return LoginResponseDto.builder()
                .user(userResponse)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getAccessToken().getExpiration())
                .build();
    }

    @Override
    @Transactional
    public RefreshTokenResponseDto refreshToken(RefreshTokenRequestDto refreshTokenRequest) {
        log.info("Xử lý refresh token");

        // Kiểm tra refresh token có hợp lệ không
        if (!jwtUtil.validateToken(refreshTokenRequest.getRefreshToken())) {
            throw new InvalidRefreshTokenException("Refresh token không hợp lệ");
        }

        // Kiểm tra refresh token có trong database không
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshTokenRequest.getRefreshToken())
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token không tồn tại"));

        // Kiểm tra refresh token có hết hạn không
        if (refreshTokenEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshTokenEntity);
            throw new InvalidRefreshTokenException("Refresh token đã hết hạn");
        }

        // Lấy thông tin user
        User user = refreshTokenEntity.getUser();
        String role = user.getRoles().stream()
                .findFirst()
                .map(roleEntity -> roleEntity.getName())
                .orElse("USER");

        // Tạo access token và refresh token mới
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), role);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), role);

        // Cập nhật refresh token trong database
        LocalDateTime newExpiryDate = LocalDateTime.now()
                .plusSeconds(jwtConfig.getRefreshToken().getExpiration() / 1000);
        refreshTokenEntity.setToken(newRefreshToken);
        refreshTokenEntity.setExpiryDate(newExpiryDate);
        refreshTokenRepository.save(refreshTokenEntity);

        return RefreshTokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getAccessToken().getExpiration())
                .build();
    }

    @Override
    @Transactional
    public void logout(Long userId) {
        log.info("Xử lý đăng xuất cho user: {}", userId);

        // Xóa refresh token của user
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public LoginResponseDto loginWithGoogleUserInfo(String email, String displayName, String avatarUrl) {
        try {
            // Kiểm tra user theo email
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                // Tạo password random (mã hoá)
                String randomPassword = generateRandomPassword(16);
                String encodedPassword = passwordEncoder.encode(randomPassword);
                user = new User();
                user.setEmail(email);
                user.setUsername(email); // Google không có username, dùng email
                user.setDisplayName(displayName);
                user.setAvatarUrl(avatarUrl);
                user.setPassword(encodedPassword);
                user.setIsActive(true);
                user.setProvider(AuthProvider.GOOGLE);
                user = userRepository.save(user);
            } else {
                if (user.getProvider() == AuthProvider.BASIC) {
                    throw new GoogleOAuth2Exception(
                            "Tài khoản này đã đăng ký truyền thống, vui lòng đăng nhập bằng form.");
                }
                // Nếu là GOOGLE thì cho đăng nhập bình thường
            }

            // Lấy role đầu tiên
            String role = user.getRoles().stream()
                    .findFirst()
                    .map(roleEntity -> roleEntity.getName())
                    .orElse("USER");

            // Sinh JWT token
            String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), role);
            String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), role);
            saveRefreshToken(user.getId(), refreshToken);
            UserResponseDto userResponse = userMapper.toUserResponseDto(user);
            return LoginResponseDto.builder()
                    .user(userResponse)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtConfig.getAccessToken().getExpiration())
                    .build();
        } catch (GoogleOAuth2Exception e) {
            throw e;
        } catch (Exception e) {
            throw new GoogleOAuth2Exception("Đăng nhập Google thất bại: " + e.getMessage(), e);
        }
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Lưu refresh token vào database
     */
    private synchronized void saveRefreshToken(Long userId, String refreshToken) {
        try {
            LocalDateTime expiryDate = LocalDateTime.now()
                    .plusSeconds(jwtConfig.getRefreshToken().getExpiration() / 1000);

            log.info("Đang lưu refresh token cho user {} với expiry date: {}", userId, expiryDate);

            // Sử dụng atomic upsert operation
            refreshTokenRepository.atomicUpsertRefreshToken(userId, refreshToken, expiryDate);

            log.info("Đã lưu refresh token thành công cho user: {}", userId);

        } catch (Exception e) {
            log.error("Lỗi khi lưu refresh token cho user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Không thể lưu refresh token", e);
        }
    }
}