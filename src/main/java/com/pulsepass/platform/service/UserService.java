package com.pulsepass.platform.service;

import com.pulsepass.platform.dto.request.RegisterUserRequest;
import com.pulsepass.platform.dto.response.UserResponse;

public interface UserService {
    UserResponse register(RegisterUserRequest request);
    UserResponse findByEmail(String email);
    UserResponse findByUsername(String username);
}