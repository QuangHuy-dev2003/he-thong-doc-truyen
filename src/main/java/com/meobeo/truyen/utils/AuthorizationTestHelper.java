package com.meobeo.truyen.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Helper class để test và debug authorization logic
 * CHỈ SỬ DỤNG TRONG DEVELOPMENT - XÓA TRONG PRODUCTION
 */
@Component
@Slf4j
public class AuthorizationTestHelper {

    /**
     * Log thông tin authorization để debug
     */
    public void logAuthorizationCheck(String operation, Long resourceId, Long userId, String userRole,
            boolean hasPermission) {
        log.info("=== AUTHORIZATION CHECK ===");
        log.info("Operation: {}", operation);
        log.info("Resource ID: {}", resourceId);
        log.info("User ID: {}", userId);
        log.info("User Role: {}", userRole);
        log.info("Has Permission: {}", hasPermission);
        log.info("========================");
    }

    /**
     * Simulated authorization scenarios để test
     */
    public void demonstrateSecurityScenarios() {
        log.info("=== SECURITY DEMONSTRATION ===");

        // Scenario 1: ADMIN có quyền với mọi resource
        logAuthorizationCheck("Lock Chapter", 123L, 1L, "ADMIN", true);

        // Scenario 2: UPLOADER chỉ có quyền với story của mình
        logAuthorizationCheck("Lock Chapter", 123L, 2L, "UPLOADER", true); // Author của story
        logAuthorizationCheck("Lock Chapter", 123L, 3L, "UPLOADER", false); // Không phải author

        // Scenario 3: USER không có quyền với bất kỳ operation nào
        logAuthorizationCheck("Lock Chapter", 123L, 4L, "USER", false);

        log.info("==============================");
    }
}
