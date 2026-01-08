package com.ornek.ehalisaha.ehalisahabackend.service;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.*;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.MembershipStatus;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.PaymentStatus;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.ReservationStatus;
import com.ornek.ehalisaha.ehalisahabackend.dto.ReservationCreateRequest;
import com.ornek.ehalisaha.ehalisahabackend.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservationService {

    private static final ZoneId FACILITY_TZ = ZoneId.of("Europe/Istanbul");

    private final PitchRepository pitchRepo;
    private final FacilityRepository facilityRepo;
    private final FacilitySlotRepository slotRepo;

    private final MembershipRepository membershipRepo;
    private final DurationOptionRepository durationRepo;
    private final PricingRuleRepository pricingRepo;
    private final ReservationRepository reservationRepo;
    private final ReservationPlayerRepository playerRepo;
    private final PaymentRepository paymentRepo;
    private final AuditService audit;

    public ReservationService(PitchRepository pitchRepo,
                              FacilityRepository facilityRepo,
                              FacilitySlotRepository slotRepo,
                              MembershipRepository membershipRepo,
                              DurationOptionRepository durationRepo,
                              PricingRuleRepository pricingRepo,
                              ReservationRepository reservationRepo,
                              ReservationPlayerRepository playerRepo,
                              PaymentRepository paymentRepo,
                              AuditService audit) {
        this.pitchRepo = pitchRepo;
        this.facilityRepo = facilityRepo;
        this.slotRepo = slotRepo;
        this.membershipRepo = membershipRepo;
        this.durationRepo = durationRepo;
        this.pricingRepo = pricingRepo;
        this.reservationRepo = reservationRepo;
        this.playerRepo = playerRepo;
        this.paymentRepo = paymentRepo;
        this.audit = audit;
    }

    @Transactional
    public Reservation create(Long userId, ReservationCreateRequest req) {

        Pitch pitch = pitchRepo.findById(req.pitchId())
                .orElseThrow(() -> new IllegalArgumentException("Pitch not found: " + req.pitchId()));

        Facility fac = facilityRepo.findById(pitch.getFacilityId())
                .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + pitch.getFacilityId()));

        Membership mem = membershipRepo.findByFacilityIdAndUserId(pitch.getFacilityId(), userId)
                .orElseThrow(() -> new SecurityException("You are not approved member of this facility"));

        if (mem.getStatus() != MembershipStatus.ACTIVE) {
            throw new SecurityException("Membership is not ACTIVE for this facility. Ask owner to approve your membership request.");
        }

        // --- SLOT VALIDATION + DURATION ---
        SlotPick slotPick = pickSlotOrThrow(fac.getId(), req.startTime(), req.durationMinutes());
        int minutes = slotPick.minutes();
        int baseDur = slotPick.baseSlotMinutes();
        int multiplier = minutes / baseDur;

        // ✅ TEK TARİFE: sadece base slot (genelde 60dk) fiyatı tutulur
        DurationOption baseOpt = durationRepo.findByMinutes(baseDur)
                .orElseThrow(() -> new IllegalArgumentException("Invalid base duration option: " + baseDur));

        PricingRule pr = pricingRepo.findByPitchIdAndDurationOptionIdAndActiveTrue(pitch.getId(), baseOpt.getId())
                .orElseThrow(() -> new IllegalStateException("Pricing not set for this pitch (base duration=" + baseDur + ")"));

        Instant end = req.startTime().plus(minutes, ChronoUnit.MINUTES);

        Reservation r = new Reservation();
        r.setPitchId(pitch.getId());
        r.setMembershipId(mem.getId());
        r.setStartTime(req.startTime());
        r.setEndTime(end);

        BigDecimal total = pr.getPrice().multiply(BigDecimal.valueOf(multiplier));
        r.setTotalPrice(total);

        r.setShuttleRequested(req.shuttle() != null && req.shuttle());

        if ("CARD".equals(req.paymentMethod().name())) r.setStatus(ReservationStatus.CONFIRMED);
        else r.setStatus(ReservationStatus.CREATED);

        Reservation saved;
        try {
            saved = reservationRepo.save(r);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Reservation overlaps with existing reservation", ex);
        }
        if (req.players() != null) {
            req.players().forEach(p -> {
                ReservationPlayer rp = new ReservationPlayer();
                rp.setReservationId(saved.getId());
                rp.setFullName(p.fullName());
                rp.setJerseyNo(p.jerseyNo());
                playerRepo.save(rp);
            });
        }

        Payment pay = new Payment();
        pay.setReservationId(saved.getId());
        pay.setMethod(req.paymentMethod());
        pay.setAmount(saved.getTotalPrice());

        if ("CARD".equals(req.paymentMethod().name())) {
            pay.setStatus(PaymentStatus.PAID);
            pay.setPaidAt(Instant.now());
            pay.setProviderRef("SIMULATED-" + saved.getId());
        } else {
            pay.setStatus(PaymentStatus.INIT);
        }
        paymentRepo.save(pay);

        audit.log(userId, "RESERVATION_CREATE", "Reservation", saved.getId(),
                "pitchId=" + pitch.getId()
                        + ", start=" + saved.getStartTime()
                        + ", minutes=" + minutes
                        + ", base=" + baseDur
                        + ", x" + multiplier
                        + ", method=" + req.paymentMethod()
                        + ", shuttle=" + saved.getShuttleRequested());

        return saved;
    }

    private record SlotPick(int slotStartMinute, int baseSlotMinutes, int minutes) {}

    private static List<FacilitySlot> defaultHourlyVirtualSlots(Long facilityId) {
        List<FacilitySlot> out = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            out.add(FacilitySlot.builder()
                    .facilityId(facilityId)
                    .startMinute(h * 60)
                    .durationMinutes(60)
                    .active(true)
                    .build());
        }
        return out;
    }

    private SlotPick pickSlotOrThrow(Long facilityId, Instant startTime, Integer requestedMinutes) {
        int startMinuteOfDay = minuteOfDayFacility(startTime);

        List<FacilitySlot> slots = slotRepo.findByFacilityIdAndActiveTrueOrderByStartMinuteAsc(facilityId);

        // DB’de hiç slot yoksa default “virtual”
        if (slots.isEmpty()) {
            slots = defaultHourlyVirtualSlots(facilityId);
        }

        FacilitySlot base = slots.stream()
                .filter(s -> s.getStartMinute() != null && s.getStartMinute() == startMinuteOfDay)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Start time must match facility slot (minuteOfDay=" + startMinuteOfDay + ")"
                ));

        int baseDur = base.getDurationMinutes();

        int finalMinutes = (requestedMinutes == null) ? baseDur : requestedMinutes;

        if (finalMinutes <= 0) throw new IllegalArgumentException("durationMinutes must be > 0");
        if (finalMinutes % baseDur != 0) {
            throw new IllegalArgumentException("durationMinutes must be multiple of slot duration (" + baseDur + ")");
        }

        return new SlotPick(base.getStartMinute(), baseDur, finalMinutes);
    }

    private static int minuteOfDayFacility(Instant t) {
        // ✅ kritik fix: Instant’ı Europe/Istanbul’a çevirip dakika hesapla
        ZonedDateTime z = t.atZone(FACILITY_TZ);
        return z.getHour() * 60 + z.getMinute();
    }
}
