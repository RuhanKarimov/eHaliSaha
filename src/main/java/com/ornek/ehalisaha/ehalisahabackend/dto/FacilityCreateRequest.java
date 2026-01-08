package com.ornek.ehalisaha.ehalisahabackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FacilityCreateRequest(
        @NotBlank(message = "Facility name is required")
        @Size(max = 120, message = "Facility name must be at most 120 chars")
        String name,

        @Size(max = 255, message = "Address must be at most 255 chars")
        String address
) {}
