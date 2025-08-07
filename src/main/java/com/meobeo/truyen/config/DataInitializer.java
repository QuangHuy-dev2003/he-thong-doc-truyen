package com.meobeo.truyen.config;

import com.meobeo.truyen.domain.entity.Role;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.enums.AuthProvider;
import com.meobeo.truyen.repository.RoleRepository;
import com.meobeo.truyen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
        // initializeAdminUser();
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

    private void initializeAdminUser() {
        log.info("Khởi tạo tài khoản ADMIN...");

        // Thay đổi email này thành Gmail của bạn
        String adminEmail = "your-email@gmail.com"; // ⚠️ THAY ĐỔI EMAIL NÀY
        String adminUsername = "admin";
        String adminPassword = "Admin@123"; // ⚠️ THAY ĐỔI PASSWORD NÀY

        // Kiểm tra tài khoản ADMIN đã tồn tại chưa
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Tài khoản ADMIN đã tồn tại với email: {}", adminEmail);
            return;
        }

        // Tạo user ADMIN
        User adminUser = new User();
        adminUser.setUsername(adminUsername);
        adminUser.setEmail(adminEmail);
        adminUser.setPassword(passwordEncoder.encode(adminPassword));
        adminUser.setDisplayName("Administrator");
        adminUser.setIsActive(true); // Kích hoạt ngay lập tức
        adminUser.setProvider(AuthProvider.BASIC);
        adminUser.setFirstLogin(false);

        // Gán role ADMIN
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("Role ADMIN không tồn tại"));

        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        adminUser.setRoles(roles);

        // Lưu user ADMIN
        User savedAdminUser = userRepository.save(adminUser);
        log.info("Đã tạo tài khoản ADMIN thành công - ID: {}, Email: {}, Username: {}",
                savedAdminUser.getId(), savedAdminUser.getEmail(), savedAdminUser.getUsername());
        log.info("⚠️  Tài khoản ADMIN mặc định - Email: {}, Password: {}", adminEmail, adminPassword);
    }
}