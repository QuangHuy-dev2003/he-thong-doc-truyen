package com.meobeo.truyen.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import jakarta.annotation.PostConstruct;

@Configuration
@PropertySource("classpath:application.properties")
public class DotenvConfig {

    /**
     * Bean để load file .env tự động khi ứng dụng khởi động
     * Sẽ tìm file .env trong thư mục gốc của project
     */
    @Bean
    public Dotenv dotenv() {
        return Dotenv.configure()
                .directory("./") // Tìm file .env trong thư mục gốc
                .filename(".env") // Tên file .env
                .ignoreIfMissing() // Không báo lỗi nếu không tìm thấy file
                .load();
    }

    /**
     * PostConstruct để đảm bảo file .env được load ngay khi ứng dụng khởi động
     */
    @PostConstruct
    public void loadDotenv() {
        try {
            Dotenv dotenv = dotenv();
            // Log để kiểm tra xem file .env có được load thành công không
            System.out.println(" File .env loaded successfully");
            System.out.println(" Environment variables loaded: " + dotenv.entries().size());
        } catch (Exception e) {
            System.err.println(" Warning: Could not load .env file: " + e.getMessage());
            System.err.println(" Make sure .env file exists in project root directory");
        }
    }
}