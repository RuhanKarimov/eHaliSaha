package com.ornek.ehalisaha.ehalisahabackend.service;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.*;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.PaymentStatus;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.ReservationStatus;
import com.ornek.ehalisaha.ehalisahabackend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OwnerReservationService {

    private static final ZoneId FACILITY_TZ = ZoneId.of("Europe/Istanbul");

    public record OwnerReservationDto(
            Long id,
            Long facilityId,
            Long pitchId,
            String pitchName,
            Instant startTime,
            Instant endTime,
            ReservationStatus status,
            String totalPrice,
            PaymentStatus paymentStatus
    ) {}

    private final ReservationRepository reservationRepo;
    private final PaymentRepository paymentRepo;
    private final PitchRepository pitchRepo;
    private final FacilityRepository facilityRepo;
    private final AuditService audit;

    public OwnerReservationService(ReservationRepository reservationRepo, PaymentRepository paymentRepo,
                                   PitchRepository pitchRepo, FacilityRepository facilityRepo,
                                   AuditService audit) {
        this.reservationRepo = reservationRepo;
        this.paymentRepo = paymentRepo;
        this.pitchRepo = pitchRepo;
        this.facilityRepo = facilityRepo;
        this.audit = audit;
    }

    public List<OwnerReservationDto> listForOwner(Long ownerId, LocalDate date, Long facilityId, Long pitchId) {

        // owner'ın facility'leri
        List<Facility> facilities = facilityRepo.findByOwnerUserId(ownerId);
        if (facilityId != null) {
            facilities = facilities.stream().filter(f -> f.getId().equals(facilityId)).toList();
        }
        if (facilities.isEmpty()) return List.of();

        List<Long> facilityIds = facilities.stream().map(Facility::getId).toList();

        // owner'ın pitch'leri
        List<Pitch> pitches = pitchRepo.findByFacilityIdIn(facilityIds);
        if (pitchId != null) {
            pitches = pitches.stream().filter(p -> p.getId().equals(pitchId)).toList();
        }
        if (pitches.isEmpty()) return List.of();

        Map<Long, Pitch> pitchMap = pitches.stream().collect(Collectors.toMap(Pitch::getId, x -> x));
        List<Long> pitchIds = pitches.stream().map(Pitch::getId).toList();

        Instant start = date.atStartOfDay(FACILITY_TZ).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(FACILITY_TZ).toInstant();

        List<Reservation> rs = reservationRepo.findOverlappingForPitches(
                pitchIds, start, end, ReservationStatus.CANCELLED
        );

        // payment status map (tek tek find yapmayalım)
        Map<Long, PaymentStatus> payMap = new HashMap<>();
        for (Reservation r : rs) {
            paymentRepo.findByReservationId(r.getId()).ifPresent(p -> payMap.put(r.getId(), p.getStatus()));
        }

        return rs.stream()
                .sorted(Comparator.comparing(Reservation::getStartTime))
                .map(r -> {
                    Pitch p = pitchMap.get(r.getPitchId());
                    Long facId = (p != null) ? p.getFacilityId() : null;
                    String pName = (p != null) ? p.getName() : ("pitch#" + r.getPitchId());
                    PaymentStatus ps = payMap.getOrDefault(r.getId(), PaymentStatus.INIT);
                    return new OwnerReservationDto(
                            r.getId(),
                            facId,
                            r.getPitchId(),
                            pName,
                            r.getStartTime(),
                            r.getEndTime(),
                            r.getStatus(),
                            r.getTotalPrice() != null ? r.getTotalPrice().toPlainString() : "0",
                            ps
                    );
                })
                .toList();
    }

    @Transactional
    public Reservation markCashPaid(Long ownerId, Long reservationId) {
        Reservation r = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        Pitch p = pitchRepo.findById(r.getPitchId())
                .orElseThrow(() -> new IllegalStateException("Pitch not found: " + r.getPitchId()));
        Facility f = facilityRepo.findById(p.getFacilityId())
                .orElseThrow(() -> new IllegalStateException("Facility not found"));
        if (!f.getOwnerUserId().equals(ownerId)) throw new SecurityException("Not your facility");

        Payment pay = paymentRepo.findByReservationId(reservationId)
                .orElseThrow(() -> new IllegalStateException("Payment not found"));

        pay.setStatus(PaymentStatus.PAID);
        pay.setPaidAt(Instant.now());
        paymentRepo.save(pay);

        r.setStatus(ReservationStatus.CONFIRMED);
        Reservation saved = reservationRepo.save(r);

        audit.log(ownerId, "CASH_PAID", "Reservation", reservationId, "confirmed");
        return saved;
    }

    @Transactional
    public Reservation cancel(Long ownerId, Long reservationId) {
        Reservation r = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        Pitch p = pitchRepo.findById(r.getPitchId())
                .orElseThrow(() -> new IllegalStateException("Pitch not found: " + r.getPitchId()));
        Facility f = facilityRepo.findById(p.getFacilityId())
                .orElseThrow(() -> new IllegalStateException("Facility not found"));
        if (!f.getOwnerUserId().equals(ownerId)) throw new SecurityException("Not your facility");

        r.setStatus(ReservationStatus.CANCELLED);
        Reservation saved = reservationRepo.save(r);

        audit.log(ownerId, "RESERVATION_CANCEL", "Reservation", reservationId, "cancelled");
        return saved;
    }

    public record ReservationNewCountDto(Long afterId, Long maxId, Long newCount) {}

    @Transactional(readOnly = true)
    public ReservationNewCountDto newReservationCount(Long ownerId, Long afterId) {
        long a = (afterId == null ? 0L : afterId);
        long maxId = reservationRepo.ownerMaxReservationId(ownerId, ReservationStatus.CANCELLED);
        long cnt = reservationRepo.ownerCountReservationsAfterId(ownerId, a, ReservationStatus.CANCELLED);
        return new ReservationNewCountDto(a, maxId, cnt);
    }



}
