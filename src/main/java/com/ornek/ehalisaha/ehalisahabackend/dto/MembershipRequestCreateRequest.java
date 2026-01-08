package com.ornek.ehalisaha.ehalisahabackend.dto;

import jakarta.validation.constraints.NotNull;

public record MembershipRequestCreateRequest(
        @NotNull(message = "facilityId is required")
        Long facilityId
) {}
