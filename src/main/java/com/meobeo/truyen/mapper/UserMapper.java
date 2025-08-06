package com.meobeo.truyen.mapper;

import com.meobeo.truyen.domain.response.auth.UserResponseDto;
import com.meobeo.truyen.domain.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponseDto toUserResponseDto(User user);
}