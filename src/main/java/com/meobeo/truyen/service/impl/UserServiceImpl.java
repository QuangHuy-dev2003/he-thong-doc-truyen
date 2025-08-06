package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.request.auth.UserRegistrationDto;
import com.meobeo.truyen.domain.response.auth.UserResponseDto;
import com.meobeo.truyen.domain.entity.Role;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.exception.UserAlreadyExistsException;
import com.meobeo.truyen.mapper.UserMapper;
import com.meobeo.truyen.repository.RoleRepository;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public UserResponseDto registerUser(UserRegistrationDto registrationDto) {
        log.info("Bắt đầu đăng ký user với username: {}", registrationDto.getUsername());

        // Kiểm tra username đã tồn tại
        if (existsByUsername(registrationDto.getUsername())) {
            throw new UserAlreadyExistsException("Username đã tồn tại");
        }

        // Kiểm tra email đã tồn tại
        if (existsByEmail(registrationDto.getEmail())) {
            throw new UserAlreadyExistsException("Email đã tồn tại");
        }

        // Tạo user mới
        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));

        // Lấy role USER mặc định
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Role USER không tồn tại"));
        user.getRoles().add(userRole);

        // Lưu user
        User savedUser = userRepository.save(user);
        log.info("Đăng ký user thành công với ID: {}", savedUser.getId());

        return userMapper.toUserResponseDto(savedUser);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}