package com.ornek.ehalisaha.ehalisahabackend.service;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.*;
import com.ornek.ehalisaha.ehalisahabackend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OwnerReservationLedgerService {

    public record PlayerDto(Long id, String fullName, Integer jerseyNo, boolean paid) {}

    public record LedgerRowDto(
            Long id,
            Long facilityId,
            Long pitchId,
            String pitchName,
            Long membershipId,
            Long memberUserId,
            String memberUsername,
            Instant startTime,
            Instant endTime,
            String status,
            List<PlayerDto> players
    ) {}

    private static final ZoneId IST = ZoneId.of("Europe/Istanbul");

    private final ReservationRepository reservationRepo;
    private final ReservationPlayerRepository playerRepo;
    private final PitchRepository pitchRepo;
    private final FacilityRepository facilityRepo;
    private final MembershipRepository membershipRepo;
    private final AppUserRepository userRepo;

    public OwnerReservationLedgerService(
            ReservationRepository reservationRepo,
            ReservationPlayerRepository playerRepo,
            PitchRepository pitchRepo,
            FacilityRepository facilityRepo,
            MembershipRepository membershipRepo,
            AppUserRepository userRepo
    ) {
        this.reservationRepo = reservationRepo;
        this.playerRepo = playerRepo;
        this.pitchRepo = pitchRepo;
        this.facilityRepo = facilityRepo;
        this.membershipRepo = membershipRepo;
        this.userRepo = userRepo;
    }

    @Transactional(readOnly = true)
    public List<LedgerRowDto> listForDay(Long ownerId, Long facilityId, Long pitchId, LocalDate date) {
        // 1) owner -> facility doğrulama
        Facility f = facilityRepo.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + facilityId));
        if (!Objects.equals(f.getOwnerUserId(), ownerId)) {
            throw new SecurityException("Not your facility");
        }

        // 2) facility'nin pitchleri
        List<Pitch> pitches = pitchRepo.findByFacilityId(facilityId);
        if (pitchId != null) {
            pitches = pitches.stream().filter(p -> Objects.equals(p.getId(), pitchId)).toList();
        }
        if (pitches.isEmpty()) return List.of();

        Map<Long, Pitch> pitchMap = pitches.stream().collect(Collectors.toMap(Pitch::getId, x -> x));
        List<Long> pitchIds = pitches.stream().map(Pitch::getId).toList();

        Instant dayStart = date.atStartOfDay(IST).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(IST).toInstant();

        // 3) reservations
        List<Reservation> reservations = reservationRepo.findByPitchIdInAndStartTimeBetween(pitchIds, dayStart, dayEnd)
                .stream()
                .sorted(Comparator.comparing(Reservation::getStartTime))
                .toList();

        if (reservations.isEmpty()) return List.of();

        List<Long> resIds = reservations.stream().map(Reservation::getId).toList();

        // 4) players bulk
        Map<Long, List<ReservationPlayer>> playersByResId = playerRepo.findByReservationIdIn(resIds)
                .stream()
                .collect(Collectors.groupingBy(ReservationPlayer::getReservationId));

        // 5) membership -> user map (batch)
        Set<Long> membershipIds = reservations.stream().map(Reservation::getMembershipId).collect(Collectors.toSet());
        Map<Long, Membership> membershipMap = membershipRepo.findAllById(membershipIds)
                .stream().collect(Collectors.toMap(Membership::getId, x -> x));

        Set<Long> userIds = membershipMap.values().stream().map(Membership::getUserId).collect(Collectors.toSet());
        Map<Long, AppUser> userMap = userRepo.findAllById(userIds)
                .stream().collect(Collectors.toMap(AppUser::getId, x -> x));

        // 6) DTO
        List<LedgerRowDto> out = new ArrayList<>();
        for (Reservation r : reservations) {
            Pitch p = pitchMap.get(r.getPitchId());
            Membership ms = membershipMap.get(r.getMembershipId());
            AppUser u = (ms != null) ? userMap.get(ms.getUserId()) : null;

            List<PlayerDto> ps = playersByResId.getOrDefault(r.getId(), List.of())
                    .stream()
                    .sorted(Comparator.comparing(ReservationPlayer::getId)) // stabil sıralama
                    .map(pl -> new PlayerDto(pl.getId(), pl.getFullName(), pl.getJerseyNo(), pl.isPaid()))
                    .toList();

            out.add(new LedgerRowDto(
                    r.getId(),
                    facilityId,
                    r.getPitchId(),
                    p != null ? p.getName() : ("pitch#" + r.getPitchId()),
                    r.getMembershipId(),
                    ms != null ? ms.getUserId() : null,
                    u != null ? u.getUsername() : null,
                    r.getStartTime(),
                    r.getEndTime(),
                    String.valueOf(r.getStatus()),
                    ps
            ));
        }

        return out;
    }

    @Transactional
    public void setPlayerPaid(Long ownerId, Long reservationId, Long playerId, boolean paid) {
        Reservation r = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        Pitch p = pitchRepo.findById(r.getPitchId())
                .orElseThrow(() -> new IllegalStateException("Pitch not found: " + r.getPitchId()));
        Facility f = facilityRepo.findById(p.getFacilityId())
                .orElseThrow(() -> new IllegalStateException("Facility not found"));

        if (!Objects.equals(f.getOwnerUserId(), ownerId)) {
            throw new SecurityException("Not your facility");
        }

        ReservationPlayer pl = playerRepo.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));

        if (!Objects.equals(pl.getReservationId(), reservationId)) {
            throw new IllegalArgumentException("Player does not belong to reservation");
        }

        pl.setPaid(paid);
        playerRepo.save(pl);
    }
}
