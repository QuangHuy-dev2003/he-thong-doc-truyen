package com.meobeo.truyen.utils;

import com.meobeo.truyen.security.CustomUserDetails;
import com.meobeo.truyen.exception.UnauthenticatedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final JwtUtil jwtUtil;

    /**
     * Lấy username từ SecurityContext
     */
    public Optional<String> getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                authentication.getPrincipal() instanceof CustomUserDetails) {
            return Optional.of(((CustomUserDetails) authentication.getPrincipal()).getUsername());
        }
        return Optional.empty();
    }

    /**
     * Lấy userId từ SecurityContext
     */
    public Optional<Long> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                authentication.getPrincipal() instanceof CustomUserDetails) {
            return Optional.of(((CustomUserDetails) authentication.getPrincipal()).getUserId());
        }
        return Optional.empty();
    }

    /**
     * Lấy CustomUserDetails từ SecurityContext
     */
    public Optional<CustomUserDetails> getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                authentication.getPrincipal() instanceof CustomUserDetails) {
            return Optional.of((CustomUserDetails) authentication.getPrincipal());
        }
        return Optional.empty();
    }

    /**
     * Lấy userId từ JWT token
     */
    public Optional<Long> getUserIdFromToken(String token) {
        try {
            if (jwtUtil.validateToken(token)) {
                return Optional.of(jwtUtil.getUserIdFromToken(token));
            }
        } catch (Exception e) {
            // Log error nếu cần
        }
        return Optional.empty();
    }

    /**
     * Kiểm tra user hiện tại có role không
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
        }
        return false;
    }

    /**
     * Kiểm tra user hiện tại có bất kỳ role nào trong danh sách
     */
    public boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getAuthorities().stream()
                    .anyMatch(authority -> {
                        for (String role : roles) {
                            if (authority.getAuthority().equals("ROLE_" + role)) {
                                return true;
                            }
                        }
                        return false;
                    });
        }
        return false;
    }

    /**
     * Kiểm tra user hiện tại có phải là ADMIN không
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Kiểm tra user hiện tại có phải là UPLOADER không
     */
    public boolean isUploader() {
        return hasRole("UPLOADER");
    }

    /**
     * Kiểm tra user hiện tại có phải là USER không
     */
    public boolean isUser() {
        return hasRole("USER");
    }

    /**
     * Lấy userId từ SecurityContext - Throw exception nếu chưa đăng nhập
     * Sử dụng phương thức này khi API bắt buộc phải có authentication
     */
    public Long getCurrentUserIdOrThrow() {
        return getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("Bạn cần đăng nhập để thực hiện thao tác này"));
    }

    /**
     * Lấy username từ SecurityContext - Throw exception nếu chưa đăng nhập
     * Sử dụng phương thức này khi API bắt buộc phải có authentication
     */
    public String getCurrentUsernameOrThrow() {
        return getCurrentUsername()
                .orElseThrow(() -> new UnauthenticatedException("Bạn cần đăng nhập để thực hiện thao tác này"));
    }

    /**
     * Lấy CustomUserDetails từ SecurityContext - Throw exception nếu chưa đăng nhập
     * Sử dụng phương thức này khi API bắt buộc phải có authentication
     */
    public CustomUserDetails getCurrentUserDetailsOrThrow() {
        return getCurrentUserDetails()
                .orElseThrow(() -> new UnauthenticatedException("Bạn cần đăng nhập để thực hiện thao tác này"));
    }
}