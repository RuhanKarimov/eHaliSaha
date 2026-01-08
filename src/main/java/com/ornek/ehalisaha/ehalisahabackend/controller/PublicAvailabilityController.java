// PublicAvailabilityController.java
package com.ornek.ehalisaha.ehalisahabackend.controller;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Facility;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Pitch;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Reservation;
import com.ornek.ehalisaha.ehalisahabackend.repository.FacilityRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.PitchRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.ReservationRepository;
import com.ornek.ehalisaha.ehalisahabackend.service.SlotService;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicAvailabilityController {

    private static final ZoneId FACILITY_TZ = ZoneId.of("Europe/Istanbul");

    public record SlotAvailabilityDto(
            int startMinute,
            int durationMinutes,
            boolean active,
            boolean occupied,
            String label
    ) {
    }

    private final PitchRepository pitchRepo;
    private final FacilityRepository facilityRepo;
    private final ReservationRepository reservationRepo;
    private final SlotService slotService;

    public PublicAvailabilityController(
            PitchRepository pitchRepo,
            FacilityRepository facilityRepo,
            ReservationRepository reservationRepo,
            SlotService slotService
    ) {
        this.pitchRepo = pitchRepo;
        this.facilityRepo = facilityRepo;
        this.reservationRepo = reservationRepo;
        this.slotService = slotService;
    }

    @GetMapping("/pitches/{pitchId}/availability")
    public List<SlotAvailabilityDto> availability(
            @PathVariable Long pitchId,
            @RequestParam String date // "2026-01-04"
    ) {
        Pitch pitch = pitchRepo.findById(pitchId)
                .orElseThrow(() -> new IllegalArgumentException("Pitch not found: " + pitchId));

        Facility fac = facilityRepo.findById(pitch.getFacilityId())
                .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + pitch.getFacilityId()));

        LocalDate d = LocalDate.parse(date);

        Instant dayStart = d.atStartOfDay(FACILITY_TZ).toInstant();
        Instant dayEnd = d.plusDays(1).atStartOfDay(FACILITY_TZ).toInstant();

        List<Reservation> reservations = reservationRepo
                .findByPitchIdAndStartTimeLessThanAndEndTimeGreaterThan(pitchId, dayEnd, dayStart);

        // Facility slotları (aktif/pasif dahil)
        List<SlotService.SlotDto> slots = slotService.publicSlots(fac.getId());

        return slots.stream().map(s -> {
            int sm = s.startMinute();
            int dm = s.durationMinutes();

            // Slotun o günkü Instant aralığı
            Instant slotStart = d.atStartOfDay(FACILITY_TZ).plusMinutes(sm).toInstant();
            Instant slotEnd = d.atStartOfDay(FACILITY_TZ).plusMinutes(sm + dm).toInstant();

            boolean occupied = reservations.stream().anyMatch(r ->
                    r.getStartTime().isBefore(slotEnd) && r.getEndTime().isAfter(slotStart)
            );

            return new SlotAvailabilityDto(
                    sm, dm, s.active(), occupied, s.label()
            );
        }).toList();
    }
}
