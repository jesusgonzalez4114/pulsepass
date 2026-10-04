package com.pulsepass.platform.service;

import com.pulsepass.platform.domain.User;
import com.pulsepass.platform.dto.request.RegisterUserRequest;
import com.pulsepass.platform.dto.response.UserResponse;
import com.pulsepass.platform.exception.BusinessRuleException;
import com.pulsepass.platform.exception.DuplicateResourceException;
import com.pulsepass.platform.mapper.UserMapper;
import com.pulsepass.platform.repository.UserRepository;
import com.pulsepass.platform.service.impl.UserServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository repository;

    @Mock
    private UserMapper mapper;

    @InjectMocks
    private UserServiceImpl service;

    @Test
    void shouldRegisterValidUser() {
        RegisterUserRequest request = new RegisterUserRequest(
                "andrea", "andrea@mail.com", "Andrea", "Gomez",
                "3001234567", "Santa Marta", LocalDate.of(2000, 1, 1));

        when(repository.existsByUsername("andrea")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("andrea@mail.com")).thenReturn(false);
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(User.class))).thenReturn(
                new UserResponse(1L, "andrea", "andrea@mail.com", true, "Andrea", "Gomez", "Santa Marta"));

        UserResponse result = service.register(request);

        assertThat(result.username()).isEqualTo("andrea");
        verify(repository).save(any(User.class));
    }

    @Test
    void shouldThrowWhenUsernameDuplicated() {
        RegisterUserRequest request = new RegisterUserRequest(
                "carlos", "carlos@mail.com", "Carlos", "Ruiz",
                "3000000000", "Bogota", LocalDate.of(1995, 5, 5));

        when(repository.existsByUsername("carlos")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEmailDuplicated() {
        RegisterUserRequest request = new RegisterUserRequest(
                "laura", "laura@mail.com", "Laura", "Diaz",
                "3000000001", "Cali", LocalDate.of(1998, 3, 3));

        when(repository.existsByUsername("laura")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("laura@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowWhenBirthDateInFuture() {
        RegisterUserRequest request = new RegisterUserRequest(
                "miguel", "miguel@mail.com", "Miguel", "Torres",
                "3000000002", "Medellin", LocalDate.now().plusDays(1));

        when(repository.existsByUsername("miguel")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("miguel@mail.com")).thenReturn(false);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(repository, never()).save(any());
    }
}