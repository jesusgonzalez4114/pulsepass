package com.pulsepass.platform.controller;

import com.pulsepass.platform.dto.request.RegisterUserRequest;
import com.pulsepass.platform.dto.response.UserResponse;
import com.pulsepass.platform.exception.DuplicateResourceException;
import com.pulsepass.platform.exception.ResourceNotFoundException;
import com.pulsepass.platform.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private static final String VALID_BODY = """
            {
              "username": "andrea",
              "email": "andrea@mail.com",
              "firstName": "Andrea",
              "lastName": "Perez",
              "phone": "3001234567",
              "city": "Santa Marta",
              "birthDate": "1995-05-10"
            }
            """;

    private static final String INVALID_EMAIL_BODY = """
            {
              "username": "andrea",
              "email": "no-es-un-email",
              "firstName": "Andrea",
              "lastName": "Perez",
              "birthDate": "1995-05-10"
            }
            """;

    private UserResponse andrea() {
        return new UserResponse(1L, "andrea", "andrea@mail.com", true,
                "Andrea", "Perez", "Santa Marta");
    }

    // TEST-CTRL-USR-001
    @Test
    void shouldReturn201WhenUserIsRegistered() throws Exception {
        when(userService.register(any(RegisterUserRequest.class))).thenReturn(andrea());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@mail.com"))
                .andExpect(jsonPath("$.active").value(true));

        verify(userService).register(any(RegisterUserRequest.class));
    }

    // TEST-CTRL-USR-002
    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_EMAIL_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.email").value("Email format is invalid"));

        verify(userService, never()).register(any());
    }

    @Test
    void shouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.username").value("Username is required"))
                .andExpect(jsonPath("$.details.email").value("Email is required"))
                .andExpect(jsonPath("$.details.firstName").value("First name is required"))
                .andExpect(jsonPath("$.details.lastName").value("Last name is required"))
                .andExpect(jsonPath("$.details.birthDate").value("Birth date is required"));

        verify(userService, never()).register(any());
    }

    @Test
    void shouldReturn400WhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));

        verify(userService, never()).register(any());
    }

    // TEST-CTRL-USR-003
    @Test
    void shouldReturn409WhenUserAlreadyExists() throws Exception {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists: andrea"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Username already exists: andrea"));
    }

    // TEST-CTRL-USR-004
    @Test
    void shouldReturn200WhenUserFoundByEmail() throws Exception {
        when(userService.findByEmail("andrea@mail.com")).thenReturn(andrea());

        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("andrea@mail.com"));

        verify(userService).findByEmail("andrea@mail.com");
    }

    @Test
    void shouldReturn404WhenEmailDoesNotExist() throws Exception {
        when(userService.findByEmail("nadie@mail.com"))
                .thenThrow(new ResourceNotFoundException("User not found: nadie@mail.com"));

        mockMvc.perform(get("/api/users/by-email").param("email", "nadie@mail.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found: nadie@mail.com"));
    }

    @Test
    void shouldReturn400WhenEmailParameterIsMissing() throws Exception {
        mockMvc.perform(get("/api/users/by-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter: email"));

        verify(userService, never()).findByEmail(any());
    }

    // TEST-CTRL-USR-005
    @Test
    void shouldReturn200WhenUserFoundByUsername() throws Exception {
        when(userService.findByUsername("andrea")).thenReturn(andrea());

        mockMvc.perform(get("/api/users/by-username").param("username", "andrea"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(userService).findByUsername("andrea");
    }
}
