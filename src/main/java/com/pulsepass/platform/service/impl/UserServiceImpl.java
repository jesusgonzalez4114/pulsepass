package com.pulsepass.platform.service.impl;



import com.pulsepass.platform.domain.User;
import com.pulsepass.platform.domain.UserProfile;
import com.pulsepass.platform.dto.request.RegisterUserRequest;
import com.pulsepass.platform.dto.response.UserResponse;
import com.pulsepass.platform.exception.BusinessRuleException;
import com.pulsepass.platform.exception.DuplicateResourceException;
import com.pulsepass.platform.exception.ResourceNotFoundException;
import com.pulsepass.platform.mapper.UserMapper;
import com.pulsepass.platform.repository.UserRepository;
import com.pulsepass.platform.service.UserService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final UserMapper mapper;

    public UserServiceImpl(UserRepository repository, UserMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {

        if (repository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already exists: " + request.username());
        }

        if (repository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Email already exists: " + request.email());
        }

        if (request.birthDate() != null && request.birthDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Birth date cannot be in the future");
        }

        User user = new User(request.username(), request.email());

        UserProfile profile = new UserProfile(
                request.firstName(), request.lastName(),
                request.phone(), request.city(), request.birthDate());

        user.assignProfile(profile);

        User saved = repository.save(user);

        return mapper.toResponse(saved);
    }

    @Override
    public UserResponse findByEmail(String email) {
        return repository.findByEmailIgnoreCase(email)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    @Override
    public UserResponse findByUsername(String username) {
        return repository.findByUsername(username)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
