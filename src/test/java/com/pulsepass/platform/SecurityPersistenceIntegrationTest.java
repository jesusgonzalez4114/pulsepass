package com.pulsepass.platform;

import com.pulsepass.platform.domain.Role;
import com.pulsepass.platform.domain.User;
import com.pulsepass.platform.domain.UserCredential;
import com.pulsepass.platform.domain.UserRole;
import com.pulsepass.platform.repository.UserCredentialRepository;
import com.pulsepass.platform.repository.UserRepository;
import com.pulsepass.platform.repository.UserRoleRepository;
import com.pulsepass.platform.security.CustomUserDetailsService;
import com.pulsepass.platform.security.SecurityPrincipal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@Testcontainers
@SpringBootTest
@Transactional
class SecurityPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("pulsepass_test")
                    .withUsername("pulsepass")
                    .withPassword("pulsepass");

    @Autowired private UserRepository userRepository;
    @Autowired private UserCredentialRepository credentialRepository;
    @Autowired private UserRoleRepository roleRepository;
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private User newUser(String username) {
        return userRepository.saveAndFlush(new User(username, username + "@mail.com"));
    }

    @Test
    void flywayShouldCreateSecurityTables() {
        Integer tables = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_name in ('user_credentials', 'user_roles')
                """, Integer.class);

        assertThat(tables).isEqualTo(2);
    }

    @Test
    void shouldPersistBcryptHashAndRolesAndLoadPrincipal() {
        User user = newUser("sec_a");
        credentialRepository.saveAndFlush(
                new UserCredential(user.getId(), "$2a$10$hashficticio", LocalDateTime.now()));
        roleRepository.saveAndFlush(new UserRole(user.getId(), Role.USER));
        roleRepository.saveAndFlush(new UserRole(user.getId(), Role.STAFF));

        SecurityPrincipal principal = userDetailsService.loadUserByUsername("sec_a");

        assertThat(principal.getUserId()).isEqualTo(user.getId());
        assertThat(principal.getPassword()).isEqualTo("$2a$10$hashficticio");
        assertThat(principal.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_STAFF");
        assertThat(userDetailsService.loadUserByUsername("sec_a@mail.com").getUserId())
                .isEqualTo(user.getId());
    }

    @Test
    void shouldRejectDuplicatedRoleForSameUser() {
        User user = newUser("sec_b");
        jdbcTemplate.update("insert into user_roles (user_id, role) values (?, 'USER')", user.getId());

        assertThatThrownBy(() ->
                jdbcTemplate.update("insert into user_roles (user_id, role) values (?, 'USER')", user.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectUnknownRoleValue() {
        User user = newUser("sec_c");

        assertThatThrownBy(() ->
                jdbcTemplate.update("insert into user_roles (user_id, role) values (?, 'SUPERUSER')", user.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectSecondCredentialForSameUser() {
        User user = newUser("sec_d");
        jdbcTemplate.update("insert into user_credentials (user_id, password_hash) values (?, 'h1')", user.getId());

        assertThatThrownBy(() ->
                jdbcTemplate.update("insert into user_credentials (user_id, password_hash) values (?, 'h2')", user.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void userWithoutCredentialCannotBeLoadedForLogin() {
        newUser("legacy_user");

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("legacy_user"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void shouldReturnRolesOfUser() {
        User user = newUser("sec_e");
        roleRepository.saveAndFlush(new UserRole(user.getId(), Role.ADMIN));

        List<Role> roles = roleRepository.findRolesByUserId(user.getId());

        assertThat(roles).containsExactly(Role.ADMIN);
    }
}
