package com.meobeo.truyen.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

@Configuration
public class JacksonConfig {

    @Value("${spring.jackson.date-format:yyyy-MM-dd HH:mm:ss}")
    private String dateFormat;

    @Value("${spring.jackson.time-zone:Asia/Ho_Chi_Minh}")
    private String timeZone;

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Đăng ký JavaTimeModule để xử lý LocalDateTime, LocalDate, etc.
        objectMapper.registerModule(new JavaTimeModule());

        // Cấu hình format ngày tháng
        objectMapper.setDateFormat(new SimpleDateFormat(dateFormat));
        objectMapper.setTimeZone(TimeZone.getTimeZone(timeZone));

        // Tắt serialization dưới dạng timestamp
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }
}
