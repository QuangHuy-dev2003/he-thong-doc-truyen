package com.meobeo.truyen.mapper;

import com.meobeo.truyen.domain.response.auth.UserResponseDto;
import com.meobeo.truyen.domain.entity.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserMapper {

    @Transactional(readOnly = true)
    public UserResponseDto toUserResponseDto(User user) {
        if (user == null) {
            return null;
        }

        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setCreatedAt(user.getCreatedAt());

        // Lấy role đầu tiên từ set roles một cách an toàn
        try {
            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                String roleName = user.getRoles().iterator().next().getName();
                dto.setRole(roleName);
            } else {
                dto.setRole("USER"); // Default role
            }
        } catch (Exception e) {
            // Fallback nếu có lỗi khi truy cập roles
            dto.setRole("USER");
        }

        return dto;
    }
}