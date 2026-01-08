package com.ornek.ehalisaha.ehalisahabackend.service;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Membership;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Reservation;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.ReservationPlayer;
import com.ornek.ehalisaha.ehalisahabackend.dto.ReservationPlayerAddRequest;
import com.ornek.ehalisaha.ehalisahabackend.repository.MembershipRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.ReservationPlayerRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReservationPlayerService {

    private final ReservationRepository reservationRepo;
    private final MembershipRepository membershipRepo;
    private final ReservationPlayerRepository playerRepo;

    public ReservationPlayerService(ReservationRepository reservationRepo,
                                    MembershipRepository membershipRepo,
                                    ReservationPlayerRepository playerRepo) {
        this.reservationRepo = reservationRepo;
        this.membershipRepo = membershipRepo;
        this.playerRepo = playerRepo;
    }

    @Transactional
    public ReservationPlayer addPlayer(Long userId, Long reservationId, ReservationPlayerAddRequest req) {
        Reservation r = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        // reservation -> membership -> user doğrulama
        Membership m = membershipRepo.findById(r.getMembershipId())
                .orElseThrow(() -> new IllegalStateException("Membership not found for reservation"));

        if (!m.getUserId().equals(userId)) {
            throw new SecurityException("This reservation does not belong to you");
        }

        ReservationPlayer p = new ReservationPlayer();
        p.setReservationId(r.getId());
        p.setFullName(req.fullName());
        p.setJerseyNo(req.jerseyNo());
        return playerRepo.save(p);
    }

    public List<ReservationPlayer> listPlayers(Long userId, Long reservationId) {
        Reservation r = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        Membership m = membershipRepo.findById(r.getMembershipId())
                .orElseThrow(() -> new IllegalStateException("Membership not found for reservation"));

        if (!m.getUserId().equals(userId)) {
            throw new SecurityException("This reservation does not belong to you");
        }

        return playerRepo.findByReservationId(reservationId);
    }
}
