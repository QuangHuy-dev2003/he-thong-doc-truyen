package com.meobeo.truyen.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private String secret;
    private AccessToken accessToken = new AccessToken();
    private RefreshToken refreshToken = new RefreshToken();

    @Data
    public static class AccessToken {
        private long expiration;
    }

    @Data
    public static class RefreshToken {
        private long expiration;
    }
}