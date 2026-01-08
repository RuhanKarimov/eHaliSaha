package com.ornek.ehalisaha.ehalisahabackend.controller;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.MatchVideo;
import com.ornek.ehalisaha.ehalisahabackend.security.AppUserPrincipal;
import com.ornek.ehalisaha.ehalisahabackend.repository.MatchVideoRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.ReservationRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/member")
@PreAuthorize("hasRole('MEMBER')")
public class MemberVideoController {

    private final ReservationRepository reservationRepo;
    private final MatchVideoRepository videoRepo;

    public MemberVideoController(ReservationRepository reservationRepo, MatchVideoRepository videoRepo) {
        this.reservationRepo = reservationRepo;
        this.videoRepo = videoRepo;
    }

    @GetMapping("/videos")
    public List<MatchVideo> myVideos(@AuthenticationPrincipal AppUserPrincipal me) {
        List<Long> resIds = reservationRepo.findReservationIdsByUserId(me.getId());
        if (resIds.isEmpty()) return Collections.emptyList();
        return videoRepo.findByReservationIdIn(resIds);
    }
}
