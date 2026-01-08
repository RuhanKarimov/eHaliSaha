package com.ornek.ehalisaha.ehalisahabackend.controller;

import com.ornek.ehalisaha.ehalisahabackend.security.AppUserPrincipal;
import com.ornek.ehalisaha.ehalisahabackend.service.OwnerReservationLedgerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/owner/reservation-ledger")
@PreAuthorize("hasRole('OWNER')")
public class OwnerReservationLedgerController {

    private final OwnerReservationLedgerService svc;

    public OwnerReservationLedgerController(OwnerReservationLedgerService svc) {
        this.svc = svc;
    }

    @GetMapping
    public List<OwnerReservationLedgerService.LedgerRowDto> list(
            @AuthenticationPrincipal AppUserPrincipal me,
            @RequestParam Long facilityId,
            @RequestParam(required = false) Long pitchId,
            @RequestParam LocalDate date
    ) {
        return svc.listForDay(me.getId(), facilityId, pitchId, date);
    }

    public record PlayerPaidPatch(boolean paid) {}

    @PatchMapping("/{reservationId}/players/{playerId}")
    public void setPaid(
            @AuthenticationPrincipal AppUserPrincipal me,
            @PathVariable Long reservationId,
            @PathVariable Long playerId,
            @RequestBody PlayerPaidPatch body
    ) {
        svc.setPlayerPaid(me.getId(), reservationId, playerId, body.paid());
    }
}
