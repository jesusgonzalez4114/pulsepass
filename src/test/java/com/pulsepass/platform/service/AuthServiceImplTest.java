package com.pulsepass.platform.service;

import com.pulsepass.platform.domain.Role;
import com.pulsepass.platform.domain.UserCredential;
import com.pulsepass.platform.domain.UserRole;
import com.pulsepass.platform.dto.request.LoginRequest;
import com.pulsepass.platform.dto.request.RegisterUserRequest;
import com.pulsepass.platform.dto.response.AuthResponse;
import com.pulsepass.platform.dto.response.UserResponse;
import com.pulsepass.platform.exception.DuplicateResourceException;
import com.pulsepass.platform.repository.UserCredentialRepository;
import com.pulsepass.platform.repository.UserRoleRepository;
import com.pulsepass.platform.security.JwtService;
import com.pulsepass.platform.security.SecurityPrincipal;
import com.pulsepass.platform.service.impl.AuthServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserService userService;
    @Mock private UserCredentialRepository credentialRepository;
    @Mock private UserRoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthServiceImpl service;

    private RegisterUserRequest registerRequest() {
        return new RegisterUserRequest("andrea", "andrea@mail.com", "Andrea", "Perez",
                "3001234567", "Santa Marta", LocalDate.of(1995, 5, 10), "ClaveFicticia-2026");
    }

    private UserResponse createdAndrea() {
        return new UserResponse(5L, "andrea", "andrea@mail.com", true, "Andrea", "Perez", "Santa Marta");
    }

    // ---------------------------------------------------------------- registro

    // TEST-AUTH-001
    @Test
    void shouldRegisterUserWithBcryptHashAndRoleUser() {
        when(userService.register(any(RegisterUserRequest.class))).thenReturn(createdAndrea());
        when(passwordEncoder.encode("ClaveFicticia-2026")).thenReturn("$2a$10$hashFicticio");

        UserResponse result = service.register(registerRequest());

        assertThat(result.id()).isEqualTo(5L);

        ArgumentCaptor<UserCredential> credential = ArgumentCaptor.forClass(UserCredential.class);
        verify(credentialRepository).save(credential.capture());
        assertThat(credential.getValue().getUserId()).isEqualTo(5L);
        assertThat(credential.getValue().getPasswordHash())
                .isEqualTo("$2a$10$hashFicticio")
                .isNotEqualTo("ClaveFicticia-2026");

        ArgumentCaptor<UserRole> role = ArgumentCaptor.forClass(UserRole.class);
        verify(roleRepository).save(role.capture());
        assertThat(role.getValue().getUserId()).isEqualTo(5L);
        assertThat(role.getValue().getRole()).isEqualTo(Role.USER);
    }

    // TEST-AUTH-002
    @Test
    void shouldNotCreateCredentialNorRoleWhenUserRegistrationFails() {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists: andrea"));

        assertThatThrownBy(() -> service.register(registerRequest()))
                .isInstanceOf(DuplicateResourceException.class);

        verifyNoInteractions(credentialRepository, roleRepository, passwordEncoder);
    }

    // ---------------------------------------------------------------- login

    private Authentication authenticatedAs(long userId) {
        SecurityPrincipal principal = new SecurityPrincipal(
                userId, "andrea", "andrea@mail.com", "$2a$10$hashFicticio", true, List.of(Role.USER));
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    // TEST-AUTH-003
    @Test
    void shouldIssueTokenOnlyAfterSuccessfulAuthentication() {
        when(authenticationManager.authenticate(any())).thenReturn(authenticatedAs(5L));
        when(jwtService.generateToken(5L)).thenReturn("jwt-ficticio");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);

        AuthResponse response = service.login(new LoginRequest("andrea", "ClaveFicticia-2026"));

        assertThat(response.accessToken()).isEqualTo("jwt-ficticio");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);

        ArgumentCaptor<Authentication> attempt = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(attempt.capture());
        assertThat(attempt.getValue().isAuthenticated()).isFalse();
        assertThat(attempt.getValue().getName()).isEqualTo("andrea");
        assertThat(attempt.getValue().getCredentials()).isEqualTo("ClaveFicticia-2026");
    }

    // TEST-AUTH-004
    @Test
    void shouldFailWithGenericErrorAndIssueNoTokenWhenPasswordIsWrong() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> service.login(new LoginRequest("andrea", "incorrecta")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(jwtService, never()).generateToken(any());
    }

    // TEST-AUTH-005
    @Test
    void shouldGiveTheSameGenericErrorForInactiveAndUnknownAccounts() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("User is disabled"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        assertThatThrownBy(() -> service.login(new LoginRequest("inactiva", "ClaveFicticia-2026")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
        assertThatThrownBy(() -> service.login(new LoginRequest("fantasma", "ClaveFicticia-2026")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(jwtService, never()).generateToken(any());
    }
}
