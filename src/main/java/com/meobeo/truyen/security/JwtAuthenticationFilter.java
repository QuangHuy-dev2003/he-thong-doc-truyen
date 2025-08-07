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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt)) {
                Long userId = jwtUtil.getUserIdFromToken(jwt);
                String username = jwtUtil.getUsernameFromToken(jwt);
                String role = jwtUtil.getRoleFromToken(jwt);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Đã xác thực user: {} với role: {}", username, role);
            }
        } catch (Exception e) {
            log.error("Không thể xác thực JWT token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Lấy JWT token từ request header hoặc cookie
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        // Thử lấy từ Authorization header trước
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // Nếu không có header, thử lấy từ cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}