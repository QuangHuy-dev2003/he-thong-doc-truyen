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
import java.util.Optional;
import java.util.stream.Collectors;

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