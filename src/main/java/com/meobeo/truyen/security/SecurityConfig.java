package com.meobeo.truyen.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meobeo.truyen.utils.ApiResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        // Mảng các endpoint công khai - không cần đăng nhập
        private static final String[] PUBLIC_ENDPOINTS = {
                        "/api/v1/auth/**",
                        "/api/v1/oauth2/callback/**",
                        "/api/v1/genres/{id}",
                        "/api/v1/genres/all",
                        "/api/v1/genres/search",
                        "/api/v1/genres/check-name/{name}",
                        "/api/v1/genres/dropdown",
                        "/api/v1/stories/{identifier}",
                        "/api/v1/stories/filter",
                        "/api/v1/stories/check-slug/{slug}",
                        "/api/v1/stories/author/{authorId}",
                        "/api/v1/stories/{storyIdentifier}/chapters",
                        "/api/v1/stories/{storyId}/chapters/{chapterNumber}",
                        "/api/v1/stories/{storyId}/chapters/{chapterNumber}/next",
                        "/api/v1/stories/{storyId}/chapters/{chapterNumber}/previous",
                        "/api/v1/stories/{storyId}/chapters/{chapterNumber}/comments",
                        "/api/v1/stories/{storyId}/comments",
                        "/api/v1/stories/{storyId}/comments/count",
                        "/api/v1/chapters/{slug}",
                        "/api/v1/chapters/{id}/next",
                        "/api/v1/chapters/{id}/previous",
                        "/api/v1/chapters/{chapterId}/comments",
                        "/api/v1/chapters/{chapterId}/comments/count",
                        "/api/v1/chapters/check-slug/{slug}",
                        "/api/v1/chapters/{chapterId}/payment",
                        "/api/v1/favorites/story/1/count",
                        "/api/v1/favorites/user/{userId}",
                        "/api/v1/subscriptions/user/{userId}",
                        "/api/v1/subscriptions/story/{storyId}/count",
                        "/api/v1/reading-history/story/{storyId}/count",
                        "/api/v1/topup-packages/all",
                        "/api/v1/topup-packages/get/{id}"
        };

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(authz -> authz
                                                // Public endpoints - không cần đăng nhập
                                                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                                                // Tất cả các endpoint khác cần đăng nhập
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                .exceptionHandling(exceptionHandling -> exceptionHandling
                                                .authenticationEntryPoint(authenticationEntryPoint())
                                                .accessDeniedHandler(accessDeniedHandler()));

                return http.build();
        }

        @Bean
        public BasicAuthenticationEntryPoint authenticationEntryPoint() {
                BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint() {
                        @Override
                        public void commence(jakarta.servlet.http.HttpServletRequest request,
                                        jakarta.servlet.http.HttpServletResponse response,
                                        org.springframework.security.core.AuthenticationException authException)
                                        throws java.io.IOException {
                                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                                ApiResponse<String> apiResponse = ApiResponse
                                                .error("Token không hợp lệ hoặc đã hết hạn");
                                String jsonResponse = new ObjectMapper().writeValueAsString(apiResponse);

                                response.getWriter().write(jsonResponse);
                        }
                };
                entryPoint.setRealmName("TiemTruyenMeoBeo");
                return entryPoint;
        }

        @Bean
        public AccessDeniedHandler accessDeniedHandler() {
                return (request, response, accessDeniedException) -> {
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                        ApiResponse<String> apiResponse = ApiResponse
                                        .error("Bạn không có quyền truy cập vào tài nguyên này");
                        String jsonResponse = new ObjectMapper().writeValueAsString(apiResponse);

                        response.getWriter().write(jsonResponse);
                };
        }
}