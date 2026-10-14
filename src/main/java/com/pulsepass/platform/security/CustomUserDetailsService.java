package com.pulsepass.platform.security;

import com.pulsepass.platform.domain.Role;
import com.pulsepass.platform.domain.User;
import com.pulsepass.platform.domain.UserCredential;
import com.pulsepass.platform.repository.UserCredentialRepository;
import com.pulsepass.platform.repository.UserRepository;
import com.pulsepass.platform.repository.UserRoleRepository;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository roleRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    UserCredentialRepository credentialRepository,
                                    UserRoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
    }

    /** Login: el identificador es un correo (si contiene '@') o un username.*/
    @Override
    public SecurityPrincipal loadUserByUsername(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new UsernameNotFoundException("Identifier is empty");
        }
        String value = identifier.trim();

        User user = (value.contains("@")
                ? userRepository.findByEmailIgnoreCase(value)
                : userRepository.findByUsername(value))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return toPrincipal(user);
    }

    /** Usado por el filtro JWT: se revalida estado y roles en cada solicitud. */
    public SecurityPrincipal loadByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toPrincipal(user);
    }

    private SecurityPrincipal toPrincipal(User user) {
        UserCredential credential = credentialRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User has no credentials"));

        List<Role> roles = roleRepository.findRolesByUserId(user.getId());

        return new SecurityPrincipal(
                user.getId(), user.getUsername(), user.getEmail(),
                credential.getPasswordHash(), user.isActive(), roles);
    }
}
