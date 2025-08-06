package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.request.auth.OtpVerificationDto;
import com.meobeo.truyen.domain.request.auth.ResendOtpDto;
import com.meobeo.truyen.domain.request.auth.UserRegistrationDto;
import com.meobeo.truyen.domain.response.auth.UserResponseDto;
import com.meobeo.truyen.domain.entity.Role;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.exception.UserAlreadyExistsException;
import com.meobeo.truyen.exception.InvalidOtpException;
import com.meobeo.truyen.exception.AccountAlreadyActivatedException;
import com.meobeo.truyen.mapper.UserMapper;
import com.meobeo.truyen.repository.RoleRepository;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.service.interfaces.EmailService;
import com.meobeo.truyen.service.interfaces.OtpService;
import com.meobeo.truyen.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final OtpService otpService;
    private final EmailService emailService;

    @Override
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
        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setIsActive(false); // Tài khoản chưa được kích hoạt

        // Lấy role USER mặc định
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Role USER không tồn tại"));
        user.getRoles().add(userRole);

        // Lưu user
        User savedUser = userRepository.save(user);
        log.info("Đăng ký user thành công với ID: {}", savedUser.getId());

        // Tạo và gửi OTP
        try {
            otpService.createAndSendOtp(savedUser.getEmail(), savedUser.getId());
            log.info("Đã gửi OTP cho user: {}", savedUser.getUsername());
        } catch (Exception e) {
            log.error("Lỗi gửi OTP cho user {}: {}", savedUser.getUsername(), e.getMessage());
            // Không throw exception để user vẫn được tạo, chỉ log lỗi
        }

        UserResponseDto userResponse = userMapper.toUserResponseDto(savedUser);
        userResponse.setRole("USER"); // Set role mặc định
        return userResponse;
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
}