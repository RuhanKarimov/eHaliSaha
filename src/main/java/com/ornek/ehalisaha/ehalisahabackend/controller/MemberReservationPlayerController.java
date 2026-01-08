package com.ornek.ehalisaha.ehalisahabackend.controller;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.ReservationPlayer;
import com.ornek.ehalisaha.ehalisahabackend.dto.ReservationPlayerAddRequest;
import com.ornek.ehalisaha.ehalisahabackend.security.AppUserPrincipal;
import com.ornek.ehalisaha.ehalisahabackend.service.ReservationPlayerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/member")
@PreAuthorize("hasRole('MEMBER')")
public class MemberReservationPlayerController {

    private final ReservationPlayerService playerService;

    public MemberReservationPlayerController(ReservationPlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/reservations/{reservationId}/players")
    public ReservationPlayer add(@AuthenticationPrincipal AppUserPrincipal me,
                                 @PathVariable Long reservationId,
                                 @Valid @RequestBody ReservationPlayerAddRequest req) {
        // service içinde “bu reservation bu user’a ait mi” kontrolü yapılmalı
        return playerService.addPlayer(me.getId(), reservationId, req);
    }

    @GetMapping("/reservations/{reservationId}/players")
    public List<ReservationPlayer> list(@AuthenticationPrincipal AppUserPrincipal me,
                                        @PathVariable Long reservationId) {
        return playerService.listPlayers(me.getId(), reservationId);
    }
}
