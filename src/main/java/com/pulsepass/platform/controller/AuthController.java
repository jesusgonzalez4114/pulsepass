package com.pulsepass.platform.controller;

import com.pulsepass.platform.dto.request.LoginRequest;
import com.pulsepass.platform.dto.response.AuthResponse;
import com.pulsepass.platform.dto.response.AuthenticatedUserResponse;
import com.pulsepass.platform.security.SecurityPrincipal;
import com.pulsepass.platform.service.AuthService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ROLE_PREFIX = "ROLE_";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> me(@AuthenticationPrincipal SecurityPrincipal principal) {
        var roles = principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority().substring(ROLE_PREFIX.length()))
                .toList();

        return ResponseEntity.ok(new AuthenticatedUserResponse(
                principal.getUserId(), principal.getUsername(), principal.getEmail(), roles));
    }
}
