package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.auth.UserRegistrationDto;
import com.meobeo.truyen.domain.response.auth.UserResponseDto;

public interface UserService {

    UserResponseDto registerUser(UserRegistrationDto registrationDto);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}