package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.EmailOtp;
import com.meobeo.truyen.repository.EmailOtpRepository;
import com.meobeo.truyen.service.interfaces.AsyncEmailService;
import com.meobeo.truyen.service.interfaces.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final EmailOtpRepository emailOtpRepository;
    private final AsyncEmailService asyncEmailService;
    private final Random random = new Random();

    @Override
    public String generateOtp() {
        // Tạo mã OTP 6 số
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    @Override
    @Transactional
    public void createAndSendOtp(String email, Long userId) {
        log.info("Tạo và gửi OTP cho user ID: {} với email: {}", userId, email);

        // Tạo OTP trong database
        EmailOtp emailOtp = createOtpInDatabase(email, userId);

        // Gửi email OTP bất đồng bộ
        asyncEmailService.sendOtpEmailAsync(email, emailOtp.getOtpCode());
    }

    @Override
    @Transactional
    public void createAndSendForgotPasswordOtp(String email, Long userId) {
        log.info("Tạo và gửi OTP quên mật khẩu cho user ID: {} với email: {}", userId, email);

        // Tạo OTP trong database
        EmailOtp emailOtp = createOtpInDatabase(email, userId);

        // Gửi email quên mật khẩu bất đồng bộ
        asyncEmailService.sendForgotPasswordEmailAsync(email, emailOtp.getOtpCode());
    }

    @Transactional
    protected EmailOtp createOtpInDatabase(String email, Long userId) {

        // Tạo OTP mới
        String otpCode = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10); // OTP có hiệu lực 10 phút

        EmailOtp emailOtp = new EmailOtp();
        emailOtp.setEmail(email);
        emailOtp.setUserId(userId);
        emailOtp.setOtpCode(otpCode);
        emailOtp.setExpiresAt(expiresAt);
        emailOtp.setIsUsed(false);

        EmailOtp savedOtp = emailOtpRepository.save(emailOtp);
        log.info("Đã tạo OTP trong database cho user ID: {}", userId);

        return savedOtp;
    }

    @Override
    public boolean verifyOtp(String email, String otpCode) {
        log.info("Xác thực OTP cho email: {}", email);

        EmailOtp emailOtp = emailOtpRepository.findValidOtpByEmailAndCode(email, otpCode, LocalDateTime.now())
                .orElse(null);

        if (emailOtp == null) {
            log.warn("OTP không hợp lệ hoặc đã hết hạn cho email: {}", email);
            return false;
        }

        if (emailOtp.getIsUsed()) {
            log.warn("OTP đã được sử dụng cho email: {}", email);
            return false;
        }

        log.info("Xác thực OTP thành công cho email: {}", email);
        return true;
    }

    @Override
    public void markOtpAsUsed(String email, String otpCode) {
        log.info("Đánh dấu OTP đã sử dụng cho email: {}", email);

        EmailOtp emailOtp = emailOtpRepository.findValidOtpByEmailAndCode(email, otpCode, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("OTP không tồn tại hoặc đã hết hạn"));

        emailOtp.setIsUsed(true);
        emailOtpRepository.save(emailOtp);

        log.info("Đã đánh dấu OTP đã sử dụng cho email: {}", email);
    }
}