package com.pulsepass.platform.service.impl;

import com.pulsepass.platform.domain.Role;
import com.pulsepass.platform.domain.UserCredential;
import com.pulsepass.platform.domain.UserRole;
import com.pulsepass.platform.dto.request.LoginRequest;
import com.pulsepass.platform.dto.request.RegisterUserRequest;
import com.pulsepass.platform.dto.response.AuthResponse;
import com.pulsepass.platform.dto.response.UserResponse;
import com.pulsepass.platform.repository.UserCredentialRepository;
import com.pulsepass.platform.repository.UserRoleRepository;
import com.pulsepass.platform.security.JwtService;
import com.pulsepass.platform.security.SecurityPrincipal;
import com.pulsepass.platform.service.AuthService;
import com.pulsepass.platform.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserService userService;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthServiceImpl(UserService userService,
                           UserCredentialRepository credentialRepository,
                           UserRoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService) {
        this.userService = userService;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Atómico: UserService.register se une a esta transacción (propagación REQUIRED).
     * Si falla el hash o el rol, se revierte también el usuario: nunca quedan cuentas sin credencial.
     * El rol siempre es USER; el cliente no puede elegirlo.
     */
    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        UserResponse created = userService.register(request);

        credentialRepository.save(new UserCredential(
                created.id(), passwordEncoder.encode(request.password()), LocalDateTime.now()));
        roleRepository.save(new UserRole(created.id(), Role.USER));

        log.info("User registered: userId={}", created.id());
        return created;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.identifier(), request.password()));
        } catch (AuthenticationException ex) {
            // Mismo mensaje para usuario inexistente, contraseña incorrecta, cuenta inactiva o sin credencial.
            log.warn("Login failed");
            throw new BadCredentialsException("Invalid credentials");
        }

        SecurityPrincipal principal = (SecurityPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal.getUserId());

        log.info("Login succeeded: userId={}", principal.getUserId());
        return new AuthResponse(token, "Bearer", jwtService.getAccessTokenTtlSeconds());
    }
}
