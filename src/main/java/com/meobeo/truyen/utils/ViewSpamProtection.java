package com.meobeo.truyen.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Utility class để xử lý bảo vệ chống spam view
 * Lấy thông tin IP, session từ request
 */
@Component
@Slf4j
public class ViewSpamProtection {

    /**
     * Lấy IP address từ request
     * Hỗ trợ proxy headers (X-Forwarded-For, X-Real-IP)
     */
    public String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes == null) {
                return "unknown";
            }

            HttpServletRequest request = attributes.getRequest();

            // Kiểm tra các header proxy
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
                return xRealIp;
            }

            return request.getRemoteAddr();
        } catch (Exception e) {
            log.warn("Không thể lấy IP address: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * Lấy session ID từ request
     */
    public String getSessionId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes == null) {
                return "unknown";
            }

            HttpServletRequest request = attributes.getRequest();
            return request.getSession().getId();
        } catch (Exception e) {
            log.warn("Không thể lấy session ID: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * Lấy User-Agent từ request
     */
    public String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes == null) {
                return "unknown";
            }

            HttpServletRequest request = attributes.getRequest();
            String userAgent = request.getHeader("User-Agent");
            return userAgent != null ? userAgent : "unknown";
        } catch (Exception e) {
            log.warn("Không thể lấy User-Agent: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * Tạo session key duy nhất cho story + session/IP
     */
    public String createSessionKey(Long storyId, String sessionId, String ipAddress) {
        return String.format("story:%d:session:%s:ip:%s", storyId, sessionId, ipAddress);
    }
}
