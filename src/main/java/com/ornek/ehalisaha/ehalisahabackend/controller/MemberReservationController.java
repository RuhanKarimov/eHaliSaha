package com.ornek.ehalisaha.ehalisahabackend.controller;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Reservation;
import com.ornek.ehalisaha.ehalisahabackend.dto.ReservationCreateRequest;
import com.ornek.ehalisaha.ehalisahabackend.security.AppUserPrincipal;
import com.ornek.ehalisaha.ehalisahabackend.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member")
@PreAuthorize("hasRole('MEMBER')")
public class MemberReservationController {

    private final ReservationService service;

    public MemberReservationController(ReservationService service) {
        this.service = service;
    }

    @PostMapping("/reservations")
    public Reservation create(@AuthenticationPrincipal AppUserPrincipal me,
                              @Valid @RequestBody ReservationCreateRequest req) {
        return service.create(me.getId(), req);
    }
}
