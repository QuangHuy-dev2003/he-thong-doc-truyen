package com.meobeo.truyen.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("AsyncTask-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "txtImportExecutor")
    public Executor txtImportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Thread pool riêng cho import TXT với nhiều thread hơn
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("TxtImport-");
        // Cho phép thread cũ bị terminate để tạo thread mới
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(300);
        executor.initialize();
        return executor;
    }

    @Bean(name = "formatFileExecutor")
    public Executor formatFileExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Thread pool riêng cho format file với ít thread hơn vì format nhẹ hơn import
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("FormatFile-");
        // Cho phép thread cũ bị terminate để tạo thread mới
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(300);
        executor.initialize();
        return executor;
    }
}