package com.ornek.ehalisaha.ehalisahabackend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PricingRuleUpsertRequest(
        @NotNull(message = "pitchId is required")
        Long pitchId,

        @NotNull(message = "durationMinutes is required")
        @Min(value = 30, message = "durationMinutes must be >= 30")
        @Max(value = 240, message = "durationMinutes must be <= 240")
        Integer durationMinutes,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.00", inclusive = false, message = "price must be > 0")
        @Digits(integer = 8, fraction = 2, message = "price format is invalid")
        BigDecimal price
) {}
