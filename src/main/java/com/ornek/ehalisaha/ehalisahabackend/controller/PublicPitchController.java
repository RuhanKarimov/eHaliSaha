package com.ornek.ehalisaha.ehalisahabackend.controller;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.*;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.ReservationStatus;
import com.ornek.ehalisaha.ehalisahabackend.repository.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/public/pitches")
public class PublicPitchController {

    private static final ZoneId FACILITY_TZ = ZoneId.of("Europe/Istanbul");

    private final ReservationRepository reservationRepo;
    private final PitchRepository pitchRepo;
    private final FacilitySlotRepository slotRepo;
    private final DurationOptionRepository durationRepo;
    private final PricingRuleRepository pricingRepo;

    public PublicPitchController(ReservationRepository reservationRepo,
                                 PitchRepository pitchRepo,
                                 FacilitySlotRepository slotRepo,
                                 DurationOptionRepository durationRepo,
                                 PricingRuleRepository pricingRepo) {
        this.reservationRepo = reservationRepo;
        this.pitchRepo = pitchRepo;
        this.slotRepo = slotRepo;
        this.durationRepo = durationRepo;
        this.pricingRepo = pricingRepo;
    }

    // ✅ Member grid “dolu/boş”
    @GetMapping("/{pitchId}/occupancy")
    public List<Integer> occupancy(@PathVariable Long pitchId,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Pitch p = pitchRepo.findById(pitchId)
                .orElseThrow(() -> new IllegalArgumentException("Pitch not found: " + pitchId));

        // gün başlangıcı/bitişi: Istanbul
        Instant start = date.atStartOfDay(FACILITY_TZ).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(FACILITY_TZ).toInstant();

        // base slot dakikası: DB slotlarından (yoksa 60)
        int baseSlot = slotRepo.findByFacilityIdAndActiveTrueOrderByStartMinuteAsc(p.getFacilityId())
                .stream()
                .map(FacilitySlot::getDurationMinutes)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(60);

        List<Reservation> rs = reservationRepo.findOverlappingForPitch(
                pitchId, start, end, ReservationStatus.CANCELLED
        );

        // rezervasyonları baseSlot parçalarına bölüp “minuteOfDay” listesi çıkar
        Set<Integer> occ = new TreeSet<>();
        for (Reservation r : rs) {
            ZonedDateTime zs = r.getStartTime().atZone(FACILITY_TZ);
            ZonedDateTime ze = r.getEndTime().atZone(FACILITY_TZ);

            int sMin = zs.getHour() * 60 + zs.getMinute();
            int eMin = ze.getHour() * 60 + ze.getMinute();

            // aynı gün dışına taşarsa kırp
            sMin = Math.max(0, sMin);
            eMin = Math.min(24 * 60, eMin);

            for (int m = sMin; m < eMin; m += baseSlot) {
                occ.add(m);
            }
        }

        return new ArrayList<>(occ);
    }

    public record PricingBaseDto(int minutes, String currency, String price) {}

    // ✅ Member fiyat görsün (tek tarife: 1 saatlik fiyat)
    @GetMapping("/{pitchId}/pricing-base")
    public PricingBaseDto pricingBase(@PathVariable Long pitchId) {

        Pitch p = pitchRepo.findById(pitchId)
                .orElseThrow(() -> new IllegalArgumentException("Pitch not found: " + pitchId));

        // base duration: facility slotlarından (yoksa 60)
        int baseSlot = slotRepo.findByFacilityIdAndActiveTrueOrderByStartMinuteAsc(p.getFacilityId())
                .stream()
                .map(FacilitySlot::getDurationMinutes)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(60);

        DurationOption baseOpt = durationRepo.findByMinutes(baseSlot)
                .orElseThrow(() -> new IllegalStateException("DurationOption missing for " + baseSlot));

        PricingRule pr = pricingRepo.findByPitchIdAndDurationOptionIdAndActiveTrue(pitchId, baseOpt.getId())
                .orElseThrow(() -> new IllegalStateException("Pricing not set for this pitch (base=" + baseSlot + ")"));

        return new PricingBaseDto(baseSlot, pr.getCurrency(), pr.getPrice().toPlainString());
    }
}
