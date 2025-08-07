package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Dọn dẹp refresh token hết hạn mỗi ngày lúc 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Bắt đầu dọn dẹp refresh token hết hạn");

        try {
            refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
            log.info("Đã dọn dẹp refresh token hết hạn");
        } catch (Exception e) {
            log.error("Lỗi khi dọn dẹp refresh token: {}", e.getMessage(), e);
        }
    }
}