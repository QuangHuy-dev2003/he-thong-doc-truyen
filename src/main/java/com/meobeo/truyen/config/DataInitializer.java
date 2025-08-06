package com.meobeo.truyen.config;

import com.meobeo.truyen.domain.entity.Role;
import com.meobeo.truyen.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
    }

    private void initializeRoles() {
        log.info("Khởi tạo các role mặc định...");

        // Tạo role ADMIN nếu chưa tồn tại
        if (!roleRepository.findByName("ADMIN").isPresent()) {
            Role adminRole = new Role();
            adminRole.setName("ADMIN");
            roleRepository.save(adminRole);
            log.info("Đã tạo role ADMIN");
        }

        // Tạo role UPLOADER nếu chưa tồn tại
        if (!roleRepository.findByName("UPLOADER").isPresent()) {
            Role uploaderRole = new Role();
            uploaderRole.setName("UPLOADER");
            roleRepository.save(uploaderRole);
            log.info("Đã tạo role UPLOADER");
        }

        // Tạo role USER nếu chưa tồn tại
        if (!roleRepository.findByName("USER").isPresent()) {
            Role userRole = new Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
            log.info("Đã tạo role USER");
        }

        log.info("Hoàn thành khởi tạo roles");
    }
}