package com.meobeo.truyen.mapper;

import com.meobeo.truyen.domain.response.auth.UserResponseDto;
import com.meobeo.truyen.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", source = "roles", qualifiedByName = "getFirstRoleName")
    @Mapping(target = "createdAt", source = "createdAt")
    UserResponseDto toUserResponseDto(User user);

    @Named("getFirstRoleName")
    default String getFirstRoleName(Set<com.meobeo.truyen.domain.entity.Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return "USER";
        }
        return roles.iterator().next().getName();
    }
}