package com.ornek.ehalisaha.ehalisahabackend.dto;

import com.ornek.ehalisaha.ehalisahabackend.domain.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record ReservationCreateRequest(
        @NotNull Long pitchId,
        @NotNull Instant startTime,
        Integer durationMinutes,              // null => slot duration (default)
        @NotNull PaymentMethod paymentMethod,
        @Valid @Size(min = 1) List<ReservationPlayerAddRequest> players,
        Boolean shuttle                       // ✅ servis bunu req.shuttle() ile okuyor
) {}
