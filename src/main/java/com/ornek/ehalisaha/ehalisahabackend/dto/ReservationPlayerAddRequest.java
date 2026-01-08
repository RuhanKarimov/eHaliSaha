package com.ornek.ehalisaha.ehalisahabackend.dto;

import jakarta.validation.constraints.*;

public record ReservationPlayerAddRequest(
        @NotBlank(message = "fullName is required")
        @Size(max = 120, message = "fullName must be at most 120 chars")
        String fullName,

        @Min(value = 0, message = "jerseyNo must be >= 0")
        @Max(value = 99, message = "jerseyNo must be <= 99")
        Integer jerseyNo
) {}
