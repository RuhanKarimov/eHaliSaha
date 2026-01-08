package com.ornek.ehalisaha.ehalisahabackend.controller;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Reservation;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.ReservationStatus;
import com.ornek.ehalisaha.ehalisahabackend.repository.ReservationRepository;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final ReservationRepository reservationRepo;
    private static final ZoneId FACILITY_TZ = ZoneId.of("Europe/Istanbul");

    public PublicController(ReservationRepository reservationRepo) {
        this.reservationRepo = reservationRepo;
    }

    @GetMapping("/ping")
    public String ping() {
        return "eHalisaha OK";
    }

    public record BusyDto(Instant startTime, Instant endTime, ReservationStatus status) {}

    // ✅ Member herkes dolu saatleri görebilsin
    // örnek: /api/public/pitches/5/busy?date=2026-01-04
    @GetMapping("/pitches/{pitchId}/busy")
    public List<BusyDto> busy(@PathVariable Long pitchId, @RequestParam String date) {
        LocalDate d = LocalDate.parse(date);
        Instant from = d.atStartOfDay(FACILITY_TZ).toInstant();
        Instant to = d.plusDays(1).atStartOfDay(FACILITY_TZ).toInstant();

        return reservationRepo
                .findByPitchIdAndStartTimeLessThanAndEndTimeGreaterThan(pitchId, to, from)
                .stream()
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .map(r -> new BusyDto(r.getStartTime(), r.getEndTime(), r.getStatus()))
                .toList();
    }
}
