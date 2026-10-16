package com.pulsepass.platform;

import com.pulsepass.platform.domain.User;
import com.pulsepass.platform.repository.UserRepository;

import org.junit.jupiter.api.Test;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("pulsepass_test")
                    .withUsername("pulsepass")
                    .withPassword("pulsepass");

    private static final String PASSWORD = "ClaveFicticia-2026";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager entityManager;

    private String registerBody(String username) {
        return """
                {
                  "username": "%s",
                  "email": "%s@mail.com",
                  "firstName": "Nombre",
                  "lastName": "Apellido",
                  "phone": "3001234567",
                  "city": "Santa Marta",
                  "birthDate": "1995-05-10",
                  "password": "%s"
                }
                """.formatted(username, username, PASSWORD);
    }

    private void register(String username) throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(registerBody(username)))
                .andExpect(status().isCreated());
    }

    private String login(String identifier, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\": \"%s\", \"password\": \"%s\"}".formatted(identifier, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private Long userId(String username) {
        return userRepository.findByUsername(username).map(User::getId).orElseThrow();
    }

    // UC-SEC-01
    @Test
    void registrationStoresABcryptHashAndRoleUserAndNeverReturnsThem() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(registerBody("andrea")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString(PASSWORD))));

        entityManager.flush(); // JDBC no ve lo que Hibernate aún no escribió
        String hash = jdbcTemplate.queryForObject(
                "select password_hash from user_credentials where user_id = ?", String.class, userId("andrea"));
        assertThat(hash).startsWith("$2").isNotEqualTo(PASSWORD);

        assertThat(jdbcTemplate.queryForList(
                "select role from user_roles where user_id = ?", String.class, userId("andrea")))
                .containsExactly("USER");
    }

    @Test
    void duplicatedRegistrationIs409AndLeavesNoExtraCredential() throws Exception {
        register("andrea");

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(registerBody("andrea")))
                .andExpect(status().isConflict());

        entityManager.flush();
        assertThat(jdbcTemplate.queryForObject("select count(*) from user_credentials", Integer.class)).isEqualTo(1);
    }

    @Test
    void registrationCannotChooseARole() throws Exception {
        String body = registerBody("intruso").replace("\"password\"", "\"role\": \"ADMIN\", \"roles\": [\"ADMIN\"], \"password\"");

        int httpStatus = mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getStatus();

        if (httpStatus == 201) {
            // Si el JSON extra se ignoró, el rol sigue siendo USER: el cliente no elige roles.
            entityManager.flush();
            assertThat(jdbcTemplate.queryForList(
                    "select role from user_roles where user_id = ?", String.class, userId("intruso")))
                    .containsExactly("USER");
        } else {
            // Si el servidor rechazó las propiedades desconocidas, tampoco se creó nada.
            assertThat(httpStatus).isEqualTo(400);
            assertThat(userRepository.findByUsername("intruso")).isEmpty();
        }
    }

    // UC-SEC-02
    @Test
    void loginByUsernameAndByEmailIssuesAValidToken() throws Exception {
        register("andrea");

        String byUsername = login("andrea", PASSWORD);
        String byEmail = login("andrea@mail.com", PASSWORD);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + byUsername))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + byEmail))
                .andExpect(status().isOk());
    }

    // T-SEC-006
    @Test
    void wrongPasswordUnknownUserAndLegacyUserGiveTheSameGeneric401() throws Exception {
        register("andrea");
        userRepository.saveAndFlush(new User("legado", "legado@mail.com")); // sin credencial

        String wrongPassword = loginFailureBody("andrea", "otra-clave-incorrecta");
        String unknownUser = loginFailureBody("fantasma", PASSWORD);
        String legacyUser = loginFailureBody("legado", PASSWORD);

        assertThat(withoutTimestamp(wrongPassword)).isEqualTo(withoutTimestamp(unknownUser));
        assertThat(withoutTimestamp(wrongPassword)).isEqualTo(withoutTimestamp(legacyUser));
    }

    private String loginFailureBody(String identifier, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\": \"%s\", \"password\": \"%s\"}".formatted(identifier, password)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
    }

    /** Compara solo lo que debe ser idéntico: estado, error y mensaje  */
    private String withoutTimestamp(String json) {
        return JsonPath.<Object>read(json, "$.status") + "|"
                + JsonPath.<String>read(json, "$.error") + "|"
                + JsonPath.<String>read(json, "$.message");
    }

    // UC-SEC-10 / T-SEC-016
    @Test
    void deactivatingAUserCutsAccessEvenWithAStillValidToken() throws Exception {
        register("andrea");
        String token = login("andrea", PASSWORD);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        User andrea = userRepository.findByUsername("andrea").orElseThrow();
        andrea.setActive(false);
        userRepository.saveAndFlush(andrea);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        loginFailureBody("andrea", PASSWORD);
    }

    // Roles vigentes
    @Test
    void rolesAreReadFromTheDatabaseOnEveryRequest() throws Exception {
        register("laura");
        String token = login("laura", PASSWORD);

        mockMvc.perform(patch("/api/tickets/TCK-NOPE/use").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        jdbcTemplate.update("insert into user_roles (user_id, role) values (?, 'STAFF')", userId("laura"));

        // Mismo token, ahora con ROLE_STAFF
        mockMvc.perform(patch("/api/tickets/TCK-NOPE/use").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // UC-SEC-08
    @Test
    void userCannotPublishButAdminReachesTheBusinessLayer() throws Exception {
        register("andrea");
        register("admin1");
        jdbcTemplate.update("insert into user_roles (user_id, role) values (?, 'ADMIN')", userId("admin1"));

        String userToken = login("andrea", PASSWORD);
        String adminToken = login("admin1", PASSWORD);

        mockMvc.perform(patch("/api/events/CMF-2026/publish").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/events/CMF-2026/publish").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // UC-SEC-06: anti-IDOR con datos reales
    @Test
    void userCannotLookUpAnotherUserButCanLookUpHimself() throws Exception {
        register("andrea");
        register("carlos");
        String token = login("andrea", PASSWORD);

        mockMvc.perform(get("/api/users/by-email").param("email", "carlos@mail.com")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@mail.com")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("andrea"));
    }

    // UC-SEC-09
    @Test
    void aTokenWithOneCharacterChangedIsRejected() throws Exception {
        register("andrea");
        String token = login("andrea", PASSWORD);
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoutesRejectRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tickets/by-user").param("email", "andrea@mail.com"))
                .andExpect(status().isUnauthorized());
    }
}
