package com.pulsepass.platform.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public record RegisterUserRequest(
        @NotBlank(message = "Username is required")
        @Size(max = 50, message = "Username must not exceed 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        String phone,

        String city,

        @NotNull(message = "Birth date is required")
        LocalDate birthDate,

        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 72, message = "Password must be between 12 and 72 characters")
        String password
) {

        /** BCrypt solo usa los primeros 72 BYTES: con tildes o emojis 72 caracteres pueden superar ese límite. */
        @AssertTrue(message = "Password must not exceed 72 bytes in UTF-8")
        public boolean isPasswordWithinBcryptLimit() {
                return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
        }

        /** Evita que la contraseña aparezca si alguien imprime el objeto en un log. */
        @Override
        public String toString() {
                return "RegisterUserRequest[username=" + username + ", email=" + email + ", password=****]";
        }
}
