package com.ornek.ehalisaha.ehalisahabackend.service;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Facility;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.FacilitySlot;
import com.ornek.ehalisaha.ehalisahabackend.repository.FacilityRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.FacilitySlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SlotService {

    public record SlotDto(Long id, int startMinute, int durationMinutes, boolean active) {
        public String label() {
            return fmt(startMinute) + "-" + fmt(startMinute + durationMinutes);
        }
        private static String fmt(int m) {
            int h = (m / 60) % 24;
            int mm = m % 60;
            return String.format("%02d:%02d", h, mm);
        }
    }

    private final FacilitySlotRepository slotRepo;
    private final FacilityRepository facilityRepo;

    public SlotService(FacilitySlotRepository slotRepo, FacilityRepository facilityRepo) {
        this.slotRepo = slotRepo;
        this.facilityRepo = facilityRepo;
    }

    public List<SlotDto> publicSlots(Long facilityId) {
        List<FacilitySlot> slots = slotRepo.findByFacilityIdAndActiveTrueOrderByStartMinuteAsc(facilityId);
        if (!slots.isEmpty()) {
            return slots.stream()
                    .map(s -> new SlotDto(s.getId(), s.getStartMinute(), s.getDurationMinutes(), Boolean.TRUE.equals(s.getActive())))
                    .toList();
        }

        // ✅ Default: 01:00 ... 23:00 (00:00 istemiyorsan böyle kalsın)
        List<SlotDto> def = new java.util.ArrayList<>();
        for (int h = 1; h < 24; h++) {
            def.add(new SlotDto(null, h * 60, 60, true));
        }
        return def;
    }

    /**
     * Owner ekranı için: aktif + kapalı tüm slotları döndür.
     * DB'de hiç slot yoksa default slotları (aktif) ver.
     */
    public List<SlotDto> ownerSlots(Long ownerUserId, Long facilityId) {
        Facility f = facilityRepo.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + facilityId));
        if (!f.getOwnerUserId().equals(ownerUserId)) throw new SecurityException("Not your facility");

        List<FacilitySlot> slots = slotRepo.findByFacilityIdOrderByStartMinuteAsc(facilityId);
        if (!slots.isEmpty()) {
            return slots.stream()
                    .map(s -> new SlotDto(s.getId(), s.getStartMinute(), s.getDurationMinutes(), Boolean.TRUE.equals(s.getActive())))
                    .toList();
        }

        List<SlotDto> def = new ArrayList<>();
        for (int h = 1; h < 24; h++) {
            def.add(new SlotDto(null, h * 60, 60, true));
        }
        return def;
    }

    @Transactional
    public List<SlotDto> ownerReplaceSlots(Long ownerUserId, Long facilityId, List<SlotDto> req) {
        Facility f = facilityRepo.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + facilityId));
        if (!f.getOwnerUserId().equals(ownerUserId)) throw new SecurityException("Not your facility");

        List<FacilitySlot> existing = slotRepo.findByFacilityIdOrderByStartMinuteAsc(facilityId);

        java.util.Map<String, FacilitySlot> exMap = new java.util.HashMap<>();
        for (FacilitySlot s : existing) {
            exMap.put(key(s.getStartMinute(), s.getDurationMinutes()), s);
        }

        java.util.Map<String, SlotDto> desired = new java.util.LinkedHashMap<>();
        if (req != null) {
            for (SlotDto s : req) {
                if (s == null) continue;
                int sm = s.startMinute();
                int dm = s.durationMinutes();

                if (sm < 0 || sm >= 24 * 60) continue;
                if (dm <= 0 || dm > 24 * 60) continue;

                desired.put(key(sm, dm), s);
            }
        }

        for (var e : desired.entrySet()) {
            SlotDto dto = e.getValue();
            FacilitySlot fs = exMap.remove(e.getKey());
            if (fs == null) {
                fs = new FacilitySlot();
                fs.setFacilityId(facilityId);
                fs.setStartMinute(dto.startMinute());
                fs.setDurationMinutes(dto.durationMinutes());
            }
            fs.setActive(dto.active());
            slotRepo.save(fs);
        }

        for (FacilitySlot leftover : exMap.values()) {
            if (Boolean.TRUE.equals(leftover.getActive())) {
                leftover.setActive(false);
                slotRepo.save(leftover);
            }
        }

        slotRepo.flush();

        // ✅ owner ekranında kapalı slotlar da görünsün
        return ownerSlots(ownerUserId, facilityId);
    }

    private static String key(Integer start, Integer dur) {
        return start + ":" + dur;
    }
}
