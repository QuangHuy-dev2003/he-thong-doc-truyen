package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.service.interfaces.AsyncEmailService;
import com.meobeo.truyen.service.interfaces.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncEmailServiceImpl implements AsyncEmailService {

    private final EmailService emailService;

    @Override
    @Async("taskExecutor")
    public void sendOtpEmailAsync(String email, String otpCode) {
        try {
            emailService.sendOtpEmail(email, otpCode);
            log.info("Gửi email OTP bất đồng bộ thành công đến: {}", email);
        } catch (Exception e) {
            log.error("Lỗi gửi email OTP bất đồng bộ đến {}: {}", email, e.getMessage());
            // Không throw exception vì đây là async method
        }
    }
}