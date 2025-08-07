package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.request.auth.ForgotPasswordDto;
import com.meobeo.truyen.domain.request.auth.OtpVerificationDto;
import com.meobeo.truyen.domain.request.auth.ResendOtpDto;
import com.meobeo.truyen.domain.request.auth.ResetPasswordDto;
import com.meobeo.truyen.domain.request.auth.UserRegistrationDto;
import com.meobeo.truyen.domain.response.auth.UserResponseDto;
import com.meobeo.truyen.domain.entity.Role;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.exception.UserAlreadyExistsException;
import com.meobeo.truyen.exception.InvalidOtpException;
import com.meobeo.truyen.exception.AccountAlreadyActivatedException;
import com.meobeo.truyen.exception.UserNotFoundException;
import com.meobeo.truyen.exception.AccountNotActivatedException;
import com.meobeo.truyen.mapper.UserMapper;
import com.meobeo.truyen.repository.RoleRepository;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.service.interfaces.OtpService;
import com.meobeo.truyen.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final OtpService otpService;

    @Override
    @Transactional
    public UserResponseDto registerUser(UserRegistrationDto registrationDto) {
        log.info("Bắt đầu đăng ký user với username: {}", registrationDto.getUsername());

        // Kiểm tra username đã tồn tại
        if (existsByUsername(registrationDto.getUsername())) {
            throw new UserAlreadyExistsException("Username đã tồn tại");
        }

        // Kiểm tra email đã tồn tại
        if (existsByEmail(registrationDto.getEmail())) {
            throw new UserAlreadyExistsException("Email đã tồn tại");
        }

        // Tạo user mới với isActive = false
        User user = createUser(registrationDto);

        // Debug log để kiểm tra user data
        log.info("User đã tạo - ID: {}, Username: {}, Email: {}, CreatedAt: {}",
                user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());

        // Tạo và gửi OTP (bất đồng bộ, không ảnh hưởng transaction)
        try {
            otpService.createAndSendOtp(user.getEmail(), user.getId());
            log.info("Đã khởi tạo quá trình gửi OTP cho user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Lỗi khởi tạo OTP cho user {}: {}", user.getUsername(), e.getMessage());
            // Không throw exception vì user đã được tạo thành công
        }

        UserResponseDto userResponse = userMapper.toUserResponseDto(user);

        // Debug log để kiểm tra response data
        log.info("UserResponse - ID: {}, Username: {}, Email: {}, Role: {}, CreatedAt: {}",
                userResponse.getId(), userResponse.getUsername(), userResponse.getEmail(),
                userResponse.getRole(), userResponse.getCreatedAt());

        return userResponse;
    }

    @Transactional
    protected User createUser(UserRegistrationDto registrationDto) {
        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setIsActive(false); // Tài khoản chưa được kích hoạt

        // Lấy role USER mặc định
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Role USER không tồn tại"));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        // Lưu user
        User savedUser = userRepository.save(user);
        log.info("Đăng ký user thành công với ID: {}", savedUser.getId());

        return savedUser;
    }

    @Override
    public boolean verifyOtpAndActivateAccount(OtpVerificationDto otpVerificationDto) {
        log.info("Xác thực OTP và kích hoạt tài khoản cho email: {}", otpVerificationDto.getEmail());

        // Xác thực OTP
        boolean isValidOtp = otpService.verifyOtp(otpVerificationDto.getEmail(), otpVerificationDto.getOtpCode());

        if (!isValidOtp) {
            log.warn("OTP không hợp lệ cho email: {}", otpVerificationDto.getEmail());
            return false;
        }

        // Tìm user theo email
        User user = userRepository.findByEmail(otpVerificationDto.getEmail())
                .orElseThrow(() -> new InvalidOtpException(
                        "Không tìm thấy user với email: " + otpVerificationDto.getEmail()));

        // Kích hoạt tài khoản
        user.setIsActive(true);
        userRepository.save(user);

        // Đánh dấu OTP đã sử dụng
        otpService.markOtpAsUsed(otpVerificationDto.getEmail(), otpVerificationDto.getOtpCode());

        log.info("Kích hoạt tài khoản thành công cho user: {}", user.getUsername());
        return true;
    }

    @Override
    @Transactional
    public void resendOtp(ResendOtpDto resendOtpDto) {
        log.info("Gửi lại OTP cho email: {}", resendOtpDto.getEmail());

        // Tìm user theo email
        User user = userRepository.findByEmail(resendOtpDto.getEmail())
                .orElseThrow(
                        () -> new InvalidOtpException("Không tìm thấy user với email: " + resendOtpDto.getEmail()));

        // Kiểm tra tài khoản chưa được kích hoạt
        if (user.getIsActive()) {
            throw new AccountAlreadyActivatedException("Tài khoản đã được kích hoạt");
        }

        // Tạo và gửi OTP mới
        otpService.createAndSendOtp(user.getEmail(), user.getId());
        log.info("Đã gửi lại OTP cho user: {}", user.getUsername());
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public void sendForgotPasswordOtp(ForgotPasswordDto forgotPasswordDto) {
        log.info("Gửi OTP quên mật khẩu cho email: {}", forgotPasswordDto.getEmail());

        // Tìm user theo email
        User user = userRepository.findByEmail(forgotPasswordDto.getEmail())
                .orElseThrow(() -> new UserNotFoundException(
                        "Không tìm thấy tài khoản với email: " + forgotPasswordDto.getEmail()));

        // Kiểm tra tài khoản đã được kích hoạt
        if (!user.getIsActive()) {
            throw new AccountNotActivatedException(
                    "Tài khoản chưa được kích hoạt. Vui lòng kích hoạt tài khoản trước.");
        }

        // Tạo và gửi OTP quên mật khẩu
        otpService.createAndSendForgotPasswordOtp(user.getEmail(), user.getId());
        log.info("Đã gửi OTP quên mật khẩu cho user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public boolean resetPassword(ResetPasswordDto resetPasswordDto) {
        log.info("Đặt lại mật khẩu cho email: {}", resetPasswordDto.getEmail());

        // Xác thực OTP
        boolean isValidOtp = otpService.verifyOtp(resetPasswordDto.getEmail(), resetPasswordDto.getOtpCode());

        if (!isValidOtp) {
            log.warn("OTP không hợp lệ cho email: {}", resetPasswordDto.getEmail());
            return false;
        }

        // Tìm user theo email
        User user = userRepository.findByEmail(resetPasswordDto.getEmail())
                .orElseThrow(() -> new UserNotFoundException(
                        "Không tìm thấy user với email: " + resetPasswordDto.getEmail()));

        // Kiểm tra tài khoản đã được kích hoạt
        if (!user.getIsActive()) {
            throw new AccountNotActivatedException(
                    "Tài khoản chưa được kích hoạt. Vui lòng kích hoạt tài khoản trước.");
        }

        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(resetPasswordDto.getNewPassword()));
        userRepository.save(user);

        // Đánh dấu OTP đã sử dụng
        otpService.markOtpAsUsed(resetPasswordDto.getEmail(), resetPasswordDto.getOtpCode());

        log.info("Đặt lại mật khẩu thành công cho user: {}", user.getUsername());
        return true;
    }
}