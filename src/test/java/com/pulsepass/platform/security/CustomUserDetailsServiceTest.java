package com.pulsepass.platform.security;

import com.pulsepass.platform.domain.Role;
import com.pulsepass.platform.domain.User;
import com.pulsepass.platform.domain.UserCredential;
import com.pulsepass.platform.repository.UserCredentialRepository;
import com.pulsepass.platform.repository.UserRepository;
import com.pulsepass.platform.repository.UserRoleRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserCredentialRepository credentialRepository;
    @Mock private UserRoleRepository roleRepository;

    @InjectMocks private CustomUserDetailsService service;

    private User andrea;

    @BeforeEach
    void setUp() {
        andrea = new User("andrea", "andrea@mail.com");
        ReflectionTestUtils.setField(andrea, "id", 7L);
    }

    private void credentialAndRolesExist(Role... roles) {
        when(credentialRepository.findById(7L))
                .thenReturn(Optional.of(new UserCredential(7L, "$2a$10$hash", LocalDateTime.now())));
        when(roleRepository.findRolesByUserId(7L)).thenReturn(List.of(roles));
    }

    // TEST-UDS-001
    @Test
    void shouldLoadUserByUsername() {
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(andrea));
        credentialAndRolesExist(Role.USER);

        SecurityPrincipal principal = service.loadUserByUsername("andrea");

        assertThat(principal.getUserId()).isEqualTo(7L);
        assertThat(principal.getUsername()).isEqualTo("andrea");
        assertThat(principal.getEmail()).isEqualTo("andrea@mail.com");
        assertThat(principal.getPassword()).isEqualTo("$2a$10$hash");
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void shouldLoadUserByEmailWhenIdentifierContainsAtSign() {
        when(userRepository.findByEmailIgnoreCase("Andrea@Mail.com")).thenReturn(Optional.of(andrea));
        credentialAndRolesExist(Role.USER);

        assertThat(service.loadUserByUsername("Andrea@Mail.com").getUserId()).isEqualTo(7L);
        verify(userRepository, never()).findByUsername("Andrea@Mail.com");
    }

    // TEST-UDS-002
    @Test
    void shouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nadie"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void shouldThrowWhenIdentifierIsBlank() {
        assertThatThrownBy(() -> service.loadUserByUsername("  "))
                .isInstanceOf(UsernameNotFoundException.class);
        assertThatThrownBy(() -> service.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // TEST-UDS-003
    @Test
    void shouldReflectCurrentRolesFromPersistence() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(andrea));
        credentialAndRolesExist(Role.USER, Role.STAFF);

        SecurityPrincipal principal = service.loadByUserId(7L);

        assertThat(principal.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_STAFF");
        assertThat(principal.hasRole(Role.STAFF)).isTrue();
        assertThat(principal.hasRole(Role.ADMIN)).isFalse();
    }

    // TEST-UDS-004
    @Test
    void shouldMarkInactiveUserAsDisabled() {
        andrea.setActive(false);
        when(userRepository.findById(7L)).thenReturn(Optional.of(andrea));
        credentialAndRolesExist(Role.USER);

        assertThat(service.loadByUserId(7L).isEnabled()).isFalse();
    }

    @Test
    void shouldThrowWhenLegacyUserHasNoCredentials() {
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(andrea));
        when(credentialRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("andrea"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void shouldThrowWhenUserIdDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadByUserId(99L))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void principalToStringShouldNeverContainTheHash() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(andrea));
        credentialAndRolesExist(Role.USER);

        assertThat(service.loadByUserId(7L).toString()).doesNotContain("$2a$10$hash");
    }
}
