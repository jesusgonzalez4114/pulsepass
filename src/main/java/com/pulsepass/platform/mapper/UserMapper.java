package com.pulsepass.platform.mapper;

import com.pulsepass.platform.domain.User;
import com.pulsepass.platform.dto.response.UserResponse;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "firstName", source = "profile.firstName")
    @Mapping(target = "lastName", source = "profile.lastName")
    @Mapping(target = "city", source = "profile.city")
    UserResponse toResponse(User user);
}