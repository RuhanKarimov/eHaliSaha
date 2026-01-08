package com.ornek.ehalisaha.ehalisahabackend.service;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.DurationOption;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Facility;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Pitch;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.PricingRule;
import com.ornek.ehalisaha.ehalisahabackend.dto.PricingRuleUpsertRequest;
import com.ornek.ehalisaha.ehalisahabackend.repository.DurationOptionRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.FacilityRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.PitchRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.PricingRuleRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests: Spring context yok. Sadece iş kuralları + repo etkileşimi.
 */
class PricingServiceTest {

    @Test
    void upsert_shouldThrow_whenPitchNotFound() {
        PitchRepository pitchRepo = mock(PitchRepository.class);
        FacilityRepository facilityRepo = mock(FacilityRepository.class);
        DurationOptionRepository durationRepo = mock(DurationOptionRepository.class);
        PricingRuleRepository pricingRepo = mock(PricingRuleRepository.class);
        AuditService audit = mock(AuditService.class);

        PricingService svc = new PricingService(pitchRepo, facilityRepo, durationRepo, pricingRepo, audit);

        when(pitchRepo.findById(99L)).thenReturn(Optional.empty());

        var req = new PricingRuleUpsertRequest(99L, 60, new BigDecimal("250"));
        assertThrows(IllegalArgumentException.class, () -> svc.upsert(1L, req));

        verifyNoInteractions(facilityRepo, durationRepo, pricingRepo, audit);
    }

    @Test
    void upsert_shouldThrow_whenNotYourFacility() {
        PitchRepository pitchRepo = mock(PitchRepository.class);
        FacilityRepository facilityRepo = mock(FacilityRepository.class);
        DurationOptionRepository durationRepo = mock(DurationOptionRepository.class);
        PricingRuleRepository pricingRepo = mock(PricingRuleRepository.class);
        AuditService audit = mock(AuditService.class);

        PricingService svc = new PricingService(pitchRepo, facilityRepo, durationRepo, pricingRepo, audit);

        Pitch pitch = new Pitch();
        pitch.setId(10L);
        pitch.setFacilityId(55L);

        Facility fac = new Facility();
        fac.setId(55L);
        fac.setOwnerUserId(999L); // başka owner

        when(pitchRepo.findById(10L)).thenReturn(Optional.of(pitch));
        when(facilityRepo.findById(55L)).thenReturn(Optional.of(fac));

        var req = new PricingRuleUpsertRequest(10L, 60, new BigDecimal("250"));
        assertThrows(SecurityException.class, () -> svc.upsert(1L, req));

        verifyNoInteractions(durationRepo, pricingRepo, audit);
    }

    @Test
    void upsert_shouldCreateRule_andLogAudit() {
        PitchRepository pitchRepo = mock(PitchRepository.class);
        FacilityRepository facilityRepo = mock(FacilityRepository.class);
        DurationOptionRepository durationRepo = mock(DurationOptionRepository.class);
        PricingRuleRepository pricingRepo = mock(PricingRuleRepository.class);
        AuditService audit = mock(AuditService.class);

        PricingService svc = new PricingService(pitchRepo, facilityRepo, durationRepo, pricingRepo, audit);

        Pitch pitch = new Pitch();
        pitch.setId(10L);
        pitch.setFacilityId(55L);

        Facility fac = new Facility();
        fac.setId(55L);
        fac.setOwnerUserId(1L);

        DurationOption opt = new DurationOption();
        opt.setId(7L);
        opt.setMinutes(60);

        when(pitchRepo.findById(10L)).thenReturn(Optional.of(pitch));
        when(facilityRepo.findById(55L)).thenReturn(Optional.of(fac));
        when(durationRepo.findByMinutes(60)).thenReturn(Optional.of(opt));
        when(pricingRepo.findByPitchIdAndDurationOptionIdAndActiveTrue(10L, 7L)).thenReturn(Optional.empty());

        when(pricingRepo.save(any(PricingRule.class))).thenAnswer(inv -> {
            PricingRule pr = inv.getArgument(0);
            pr.setId(123L);
            return pr;
        });

        var req = new PricingRuleUpsertRequest(10L, 60, new BigDecimal("300"));
        PricingRule saved = svc.upsert(1L, req);

        assertEquals(123L, saved.getId());
        assertEquals(10L, saved.getPitchId());
        assertEquals(7L, saved.getDurationOptionId());
        assertEquals(new BigDecimal("300"), saved.getPrice());
        assertTrue(Boolean.TRUE.equals(saved.getActive()));

        verify(audit).log(eq(1L), eq("PRICING_UPSERT"), eq("PricingRule"), eq(123L), anyString());
    }

    @Test
    void listForOwner_shouldReturnRules() {
        PitchRepository pitchRepo = mock(PitchRepository.class);
        FacilityRepository facilityRepo = mock(FacilityRepository.class);
        DurationOptionRepository durationRepo = mock(DurationOptionRepository.class);
        PricingRuleRepository pricingRepo = mock(PricingRuleRepository.class);
        AuditService audit = mock(AuditService.class);

        PricingService svc = new PricingService(pitchRepo, facilityRepo, durationRepo, pricingRepo, audit);

        Pitch pitch = new Pitch();
        pitch.setId(10L);
        pitch.setFacilityId(55L);

        Facility fac = new Facility();
        fac.setId(55L);
        fac.setOwnerUserId(1L);

        PricingRule r1 = new PricingRule(); r1.setId(1L);
        PricingRule r2 = new PricingRule(); r2.setId(2L);

        when(pitchRepo.findById(10L)).thenReturn(Optional.of(pitch));
        when(facilityRepo.findById(55L)).thenReturn(Optional.of(fac));
        when(pricingRepo.findByPitchId(10L)).thenReturn(List.of(r1, r2));

        List<PricingRule> out = svc.listForOwner(1L, 10L);
        assertEquals(2, out.size());

        verifyNoInteractions(durationRepo, audit);
    }
}
