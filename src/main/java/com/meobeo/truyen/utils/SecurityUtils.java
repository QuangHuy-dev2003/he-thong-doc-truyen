package com.meobeo.truyen.utils;

import com.meobeo.truyen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /**
     * Lấy username từ SecurityContext
     */
    public Optional<String> getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return Optional.of(authentication.getName());
        }
        return Optional.empty();
    }

    /**
     * Lấy userId từ SecurityContext
     */
    public Optional<Long> getCurrentUserId() {
        return getCurrentUsername()
                .flatMap(username -> userRepository.findByUsername(username))
                .map(user -> user.getId());
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
}