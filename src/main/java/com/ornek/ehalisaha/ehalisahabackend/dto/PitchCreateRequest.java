package com.ornek.ehalisaha.ehalisahabackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PitchCreateRequest(
        @NotBlank(message = "Pitch name is required")
        @Size(max = 120, message = "Pitch name must be at most 120 chars")
        String name,
        @NotNull Long facilityId
) {}
