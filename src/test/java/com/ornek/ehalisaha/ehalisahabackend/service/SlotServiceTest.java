package com.ornek.ehalisaha.ehalisahabackend.service;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Facility;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.FacilitySlot;
import com.ornek.ehalisaha.ehalisahabackend.repository.FacilityRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.FacilitySlotRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SlotService için gerçekçi unit testler.
 * - DB'de slot yoksa default slotlar dönmeli
 * - Owner replace senaryosu: removed slotlar otomatik kapatılmalı
 */
class SlotServiceTest {

    @Test
    void publicSlots_shouldReturnDefault_23Slots_whenDbEmpty() {
        FacilitySlotRepository slotRepo = mock(FacilitySlotRepository.class);
        FacilityRepository facilityRepo = mock(FacilityRepository.class);

        SlotService svc = new SlotService(slotRepo, facilityRepo);

        when(slotRepo.findByFacilityIdAndActiveTrueOrderByStartMinuteAsc(10L)).thenReturn(List.of());

        var out = svc.publicSlots(10L);
        assertEquals(23, out.size(), "Default slots: 01:00..23:00 => 23 adet");
        assertEquals("01:00-02:00", out.get(0).label());
        assertEquals("23:00-00:00", out.get(out.size() - 1).label());

        verifyNoInteractions(facilityRepo);
    }

    @Test
    void ownerSlots_shouldThrow_whenNotOwner() {
        FacilitySlotRepository slotRepo = mock(FacilitySlotRepository.class);
        FacilityRepository facilityRepo = mock(FacilityRepository.class);

        SlotService svc = new SlotService(slotRepo, facilityRepo);

        Facility f = new Facility();
        f.setId(10L);
        f.setOwnerUserId(999L);

        when(facilityRepo.findById(10L)).thenReturn(Optional.of(f));

        assertThrows(SecurityException.class, () -> svc.ownerSlots(1L, 10L));
        verifyNoInteractions(slotRepo);
    }

    @Test
    void ownerReplaceSlots_shouldDeactivateLeftoverSlots() {
        FacilitySlotRepository slotRepo = mock(FacilitySlotRepository.class);
        FacilityRepository facilityRepo = mock(FacilityRepository.class);

        SlotService svc = new SlotService(slotRepo, facilityRepo);

        Facility f = new Facility();
        f.setId(10L);
        f.setOwnerUserId(1L);
        when(facilityRepo.findById(10L)).thenReturn(Optional.of(f));

        // mevcut DB slotları: 01:00 ve 02:00 aktif
        FacilitySlot s1 = new FacilitySlot();
        s1.setId(1L);
        s1.setFacilityId(10L);
        s1.setStartMinute(60);
        s1.setDurationMinutes(60);
        s1.setActive(true);

        FacilitySlot s2 = new FacilitySlot();
        s2.setId(2L);
        s2.setFacilityId(10L);
        s2.setStartMinute(120);
        s2.setDurationMinutes(60);
        s2.setActive(true);

        List<FacilitySlot> db = new ArrayList<>(List.of(s1, s2));

        when(slotRepo.findByFacilityIdOrderByStartMinuteAsc(10L)).thenAnswer(inv -> db);

        // save() çağrıları db listesini güncellesin
        when(slotRepo.save(any(FacilitySlot.class))).thenAnswer(inv -> {
            FacilitySlot in = inv.getArgument(0);
            FacilitySlot match = db.stream()
                    .filter(x -> x.getStartMinute().equals(in.getStartMinute()) && x.getDurationMinutes().equals(in.getDurationMinutes()))
                    .findFirst().orElse(null);
            if (match == null) {
                db.add(in);
                return in;
            }
            match.setActive(in.getActive());
            return match;
        });

        // owner ekranında geri dönüş için aynı repo çağrısı tekrar kullanılacak
        when(slotRepo.findByFacilityIdAndActiveTrueOrderByStartMinuteAsc(10L))
                .thenReturn(List.of());

        // request sadece 01:00 slotunu aktif bırakıyor (02:00 kaldırıldı => otomatik pasiflenmeli)
        var req = List.of(new SlotService.SlotDto(null, 60, 60, true));

        var out = svc.ownerReplaceSlots(1L, 10L, req);

        // 02:00 slotu kapatılmış olmalı
        FacilitySlot afterS2 = db.stream().filter(x -> x.getStartMinute() == 120).findFirst().orElseThrow();
        assertFalse(Boolean.TRUE.equals(afterS2.getActive()), "Kaldırılan slot otomatik pasiflenmeli");

        // ownerReplaceSlots, ownerSlots() döndürdüğü için boş DB varsayımıyla default dönebilir.
        // Burada önemli olan: pasifleştirme + save çağrısı.
        verify(slotRepo, atLeast(2)).save(any(FacilitySlot.class));
        verify(slotRepo).flush();
    }
}
