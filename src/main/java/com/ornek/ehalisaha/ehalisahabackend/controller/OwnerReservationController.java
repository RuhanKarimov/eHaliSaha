package com.ornek.ehalisaha.ehalisahabackend.controller;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Reservation;
import com.ornek.ehalisaha.ehalisahabackend.security.AppUserPrincipal;
import com.ornek.ehalisaha.ehalisahabackend.service.OwnerReservationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/owner")
@PreAuthorize("hasRole('OWNER')")
public class OwnerReservationController {

    private final OwnerReservationService service;

    public OwnerReservationController(OwnerReservationService service) {
        this.service = service;
    }

    // ✅ Owner rezervasyon listesini görsün
    @GetMapping("/reservations")
    public List<OwnerReservationService.OwnerReservationDto> list(
            @AuthenticationPrincipal AppUserPrincipal me,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long facilityId,
            @RequestParam(required = false) Long pitchId
    ) {
        return service.listForOwner(me.getId(), date, facilityId, pitchId);
    }

    @PostMapping("/reservations/{id}/cash-paid")
    public Reservation cashPaid(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) {
        return service.markCashPaid(me.getId(), id);
    }

    @PostMapping("/reservations/{id}/cancel")
    public Reservation cancel(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) {
        return service.cancel(me.getId(), id);
    }

    @GetMapping("/reservations/new-count")
    public OwnerReservationService.ReservationNewCountDto newCount(
            @AuthenticationPrincipal AppUserPrincipal me,
            @RequestParam(name = "afterId", defaultValue = "0") Long afterId
    ) {
        return service.newReservationCount(me.getId(), afterId);
    }

}
