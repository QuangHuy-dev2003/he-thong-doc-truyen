package com.meobeo.truyen.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình cache cho analytics
 * Sử dụng in-memory cache cho các analytics phổ biến (7/30 ngày)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();

        // Cache cho analytics theo ngày (7 ngày)
        cacheManager.setCacheNames(java.util.Arrays.asList(
                "analytics-daily-7d",
                "analytics-weekly-4w",
                "analytics-monthly-12m",
                "top-stories-7d",
                "top-stories-30d"));

        return cacheManager;
    }
}
