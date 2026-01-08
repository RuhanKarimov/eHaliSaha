package com.ornek.ehalisaha.ehalisahabackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 60, message = "username length must be 3-60")
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "username can contain only letters, digits, dot, underscore and dash"
        )
        String username,

        @NotBlank(message = "password is required")
        @Size(min = 6, max = 60, message = "password length must be 6-60")
        String password
) {}
