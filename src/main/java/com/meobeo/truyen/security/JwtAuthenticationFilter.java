package com.meobeo.truyen.security;

import com.meobeo.truyen.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.debug("=== JWT FILTER START ===");
        log.debug("Request URI: {}", request.getRequestURI());
        log.debug("Request method: {}", request.getMethod());
        log.debug("Request headers: Authorization={}", request.getHeader("Authorization"));

        // Luôn clear SecurityContext trước khi xử lý
        SecurityContextHolder.clearContext();
        log.debug("SecurityContext cleared");

        try {
            String jwt = getJwtFromRequest(request);
            log.debug("JWT token extracted: {}", jwt != null ? "YES" : "NO");

            if (StringUtils.hasText(jwt)) {
                log.debug("JWT token found, length: {}", jwt.length());
                log.debug("JWT token starts with: {}", jwt.substring(0, Math.min(20, jwt.length())));

                log.debug("Validating JWT token...");
                boolean isValid = jwtUtil.validateToken(jwt);
                log.debug("JWT validation result: {}", isValid);

                if (isValid) {
                    try {
                        Long userId = jwtUtil.getUserIdFromToken(jwt);
                        log.debug("Valid JWT token found, userId: {}", userId);

                        String username = jwtUtil.getUsernameFromToken(jwt);
                        String role = jwtUtil.getRoleFromToken(jwt);
                        log.debug("JWT claims - username: {}, role: {}", username, role);

                        // Load user details từ database để có thông tin roles đầy đủ
                        log.debug("Loading user details from database...");
                        CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService
                                .loadUserById(userId);
                        log.debug("User details loaded successfully: {}", userDetails.getUsername());
                        log.debug("User authorities: {}", userDetails.getAuthorities());

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("Authentication set successfully for user: {} with roles: {}",
                                userDetails.getUsername(), userDetails.getAuthorities());
                    } catch (Exception e) {
                        log.error("Lỗi khi load user details: {}", e.getMessage(), e);
                        SecurityContextHolder.clearContext();
                        log.debug("SecurityContext cleared due to user loading error");
                    }
                } else {
                    log.debug("JWT token không hợp lệ cho request: {}", request.getRequestURI());
                    SecurityContextHolder.clearContext();
                    log.debug("SecurityContext cleared due to invalid JWT");
                }
            } else {
                log.debug("Không có JWT token cho request: {}", request.getRequestURI());
                SecurityContextHolder.clearContext();
                log.debug("SecurityContext cleared due to no JWT token");
            }
        } catch (Exception e) {
            log.error("Không thể xác thực JWT token: {}", e.getMessage(), e);
            // Clear context nếu có lỗi
            SecurityContextHolder.clearContext();
            log.debug("SecurityContext cleared due to exception");
        }

        // Log final authentication state
        var finalAuth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Final authentication state: {}", finalAuth != null ? finalAuth.getPrincipal() : "NULL");
        log.debug("=== JWT FILTER END ===");

        filterChain.doFilter(request, response);
    }

    /**
     * Lấy JWT token từ request header hoặc cookie
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        // Thử lấy từ Authorization header trước
        String bearerToken = request.getHeader("Authorization");
        log.debug("Authorization header: {}", bearerToken);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            log.debug("Extracted JWT token from header: {}", token.substring(0, Math.min(20, token.length())) + "...");
            return token;
        }

        // Nếu không có header, thử lấy từ cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    log.debug("Found JWT token in cookie: {}",
                            cookie.getValue().substring(0, Math.min(20, cookie.getValue().length())) + "...");
                    return cookie.getValue();
                }
            }
        }

        log.debug("No JWT token found in request");
        return null;
    }
}