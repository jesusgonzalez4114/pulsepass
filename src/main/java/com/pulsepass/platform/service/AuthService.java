package com.pulsepass.platform.service;

import com.pulsepass.platform.dto.request.LoginRequest;
import com.pulsepass.platform.dto.request.RegisterUserRequest;
import com.pulsepass.platform.dto.response.AuthResponse;
import com.pulsepass.platform.dto.response.UserResponse;

public interface AuthService {

    /** Crea User + perfil + credencial BCrypt + ROLE_USER en una sola transacción. */
    UserResponse register(RegisterUserRequest request);

    /** Autentica y emite un JWT. Ante cualquier fallo lanza AuthenticationException genérica. */
    AuthResponse login(LoginRequest request);
}
