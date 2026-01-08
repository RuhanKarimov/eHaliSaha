package com.ornek.ehalisaha.ehalisahabackend.service;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Facility;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Pitch;
import com.ornek.ehalisaha.ehalisahabackend.dto.FacilityCreateRequest;
import com.ornek.ehalisaha.ehalisahabackend.dto.PitchCreateRequest;
import com.ornek.ehalisaha.ehalisahabackend.repository.FacilityRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.PitchRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OwnerOpsService için unit testler.
 * Owner'ın kendi tesis/saha yönetimi mantığını doğrular.
 */
class OwnerOpsServiceTest {

    @Test
    void createFacility_shouldThrow_whenDuplicateNameForOwner() {
        FacilityRepository facilityRepo = mock(FacilityRepository.class);
        PitchRepository pitchRepo = mock(PitchRepository.class);
        AuditService audit = mock(AuditService.class);

        OwnerOpsService svc = new OwnerOpsService(facilityRepo, pitchRepo, audit);

        when(facilityRepo.existsByOwnerUserIdAndNameKey(eq(1L), anyString())).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> svc.createFacility(1L, new FacilityCreateRequest("Arena", "Merkez")));

        verify(facilityRepo, never()).save(any());
        verifyNoInteractions(pitchRepo, audit);
    }

    @Test
    void createPitch_shouldThrow_whenFacilityNotOwnedByCaller() {
        FacilityRepository facilityRepo = mock(FacilityRepository.class);
        PitchRepository pitchRepo = mock(PitchRepository.class);
        AuditService audit = mock(AuditService.class);

        OwnerOpsService svc = new OwnerOpsService(facilityRepo, pitchRepo, audit);

        Facility f = new Facility();
        f.setId(10L);
        f.setOwnerUserId(999L);

        when(facilityRepo.findById(10L)).thenReturn(Optional.of(f));
        when(pitchRepo.existsByFacilityIdAndNameKey(eq(10L), anyString())).thenReturn(false);

        assertThrows(SecurityException.class,
                () -> svc.createPitch(1L, 10L, new PitchCreateRequest("Saha A", 10L)));

        verify(pitchRepo, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void createPitch_shouldThrow_whenBodyFacilityIdDoesNotMatchPath() {
        FacilityRepository facilityRepo = mock(FacilityRepository.class);
        PitchRepository pitchRepo = mock(PitchRepository.class);
        AuditService audit = mock(AuditService.class);

        OwnerOpsService svc = new OwnerOpsService(facilityRepo, pitchRepo, audit);

        Facility f = new Facility();
        f.setId(10L);
        f.setOwnerUserId(1L);

        when(facilityRepo.findById(10L)).thenReturn(Optional.of(f));
        when(pitchRepo.existsByFacilityIdAndNameKey(eq(10L), anyString())).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> svc.createPitch(1L, 10L, new PitchCreateRequest("Saha A", 999L)));

        verify(pitchRepo, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void createPitch_shouldSavePitch_andAudit_whenOk() {
        FacilityRepository facilityRepo = mock(FacilityRepository.class);
        PitchRepository pitchRepo = mock(PitchRepository.class);
        AuditService audit = mock(AuditService.class);

        OwnerOpsService svc = new OwnerOpsService(facilityRepo, pitchRepo, audit);

        Facility f = new Facility();
        f.setId(10L);
        f.setOwnerUserId(1L);

        when(facilityRepo.findById(10L)).thenReturn(Optional.of(f));
        when(pitchRepo.existsByFacilityIdAndNameKey(eq(10L), anyString())).thenReturn(false);

        when(pitchRepo.save(any(Pitch.class))).thenAnswer(inv -> {
            Pitch p = inv.getArgument(0);
            p.setId(77L);
            return p;
        });

        Pitch saved = svc.createPitch(1L, 10L, new PitchCreateRequest("Saha A", 10L));
        assertEquals(77L, saved.getId());
        assertEquals(10L, saved.getFacilityId());
        assertEquals("Saha A", saved.getName());

        verify(audit).log(eq(1L), eq("PITCH_CREATE"), eq("Pitch"), eq(77L), anyString());
    }
}
