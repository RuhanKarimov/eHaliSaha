package com.ornek.ehalisaha.ehalisahabackend.service;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.*;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.MembershipStatus;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.PaymentMethod;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.PaymentStatus;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.ReservationStatus;
import com.ornek.ehalisaha.ehalisahabackend.dto.ReservationCreateRequest;
import com.ornek.ehalisaha.ehalisahabackend.dto.ReservationPlayerAddRequest;
import com.ornek.ehalisaha.ehalisahabackend.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ✅ Unit test: Spring context yok.
 * Amaç: ReservationService.create() iş kuralları doğru mu?
 */
class ReservationServiceTest {

    @Test
    void create_shouldThrow_whenMembershipNotActive() {
        // mocks
        PitchRepository pitchRepo = mock(PitchRepository.class);
        FacilityRepository facilityRepo = mock(FacilityRepository.class);
        FacilitySlotRepository slotRepo = mock(FacilitySlotRepository.class);
        MembershipRepository membershipRepo = mock(MembershipRepository.class);
        DurationOptionRepository durationRepo = mock(DurationOptionRepository.class);
        PricingRuleRepository pricingRepo = mock(PricingRuleRepository.class);
        ReservationRepository reservationRepo = mock(ReservationRepository.class);
        ReservationPlayerRepository playerRepo = mock(ReservationPlayerRepository.class);
        PaymentRepository paymentRepo = mock(PaymentRepository.class);
        AuditService audit = mock(AuditService.class);

        ReservationService svc = new ReservationService(
                pitchRepo, facilityRepo, slotRepo,
                membershipRepo, durationRepo, pricingRepo,
                reservationRepo, playerRepo, paymentRepo, audit
        );

        Pitch pitch = new Pitch();
        pitch.setId(1L);
        pitch.setFacilityId(10L);

        Facility fac = new Facility();
        fac.setId(10L);

        Membership mem = new Membership();
        mem.setId(99L);
        mem.setFacilityId(10L);
        mem.setUserId(7L);
        // Projede MembershipStatus enum'unda PENDING yok.
        // ACTIVE dışındaki herhangi bir durum rezervasyonu engellemeli.
        mem.setStatus(MembershipStatus.SUSPENDED);

        when(pitchRepo.findById(1L)).thenReturn(Optional.of(pitch));
        when(facilityRepo.findById(10L)).thenReturn(Optional.of(fac));
        when(membershipRepo.findByFacilityIdAndUserId(10L, 7L)).thenReturn(Optional.of(mem));

        var req = new ReservationCreateRequest(
                1L,
                ZonedDateTime.of(2030, 1, 1, 10, 0, 0, 0, ZoneId.of("Europe/Istanbul")).toInstant(),
                60,
                PaymentMethod.CARD,
                List.of(new ReservationPlayerAddRequest("Ali Veli", 10)),
                false
        );

        SecurityException ex = assertThrows(SecurityException.class, () -> svc.create(7L, req));
        assertTrue(ex.getMessage().toUpperCase().contains("ACTIVE"));

        verifyNoInteractions(durationRepo, pricingRepo, reservationRepo, paymentRepo, audit);
    }

    @Test
    void create_shouldComputeTotalPrice_andCreatePaidPayment_whenCard() {
        // mocks
        PitchRepository pitchRepo = mock(PitchRepository.class);
        FacilityRepository facilityRepo = mock(FacilityRepository.class);
        FacilitySlotRepository slotRepo = mock(FacilitySlotRepository.class);
        MembershipRepository membershipRepo = mock(MembershipRepository.class);
        DurationOptionRepository durationRepo = mock(DurationOptionRepository.class);
        PricingRuleRepository pricingRepo = mock(PricingRuleRepository.class);
        ReservationRepository reservationRepo = mock(ReservationRepository.class);
        ReservationPlayerRepository playerRepo = mock(ReservationPlayerRepository.class);
        PaymentRepository paymentRepo = mock(PaymentRepository.class);
        AuditService audit = mock(AuditService.class);

        ReservationService svc = new ReservationService(
                pitchRepo, facilityRepo, slotRepo,
                membershipRepo, durationRepo, pricingRepo,
                reservationRepo, playerRepo, paymentRepo, audit
        );

        // domain objects
        Pitch pitch = new Pitch();
        pitch.setId(1L);
        pitch.setFacilityId(10L);

        Facility fac = new Facility();
        fac.setId(10L);

        Membership mem = new Membership();
        mem.setId(99L);
        mem.setFacilityId(10L);
        mem.setUserId(7L);
        mem.setStatus(MembershipStatus.ACTIVE);

        DurationOption baseOpt = new DurationOption();
        baseOpt.setId(5L);
        baseOpt.setMinutes(60);

        PricingRule pr = new PricingRule();
        pr.setId(11L);
        pr.setPitchId(1L);
        pr.setDurationOptionId(5L);
        pr.setActive(true);
        pr.setPrice(new BigDecimal("100"));

        when(pitchRepo.findById(1L)).thenReturn(Optional.of(pitch));
        when(facilityRepo.findById(10L)).thenReturn(Optional.of(fac));
        when(membershipRepo.findByFacilityIdAndUserId(10L, 7L)).thenReturn(Optional.of(mem));

        // slotRepo boş -> servis default 24 saatlik virtual slot kullanacak
        when(slotRepo.findByFacilityIdAndActiveTrueOrderByStartMinuteAsc(10L)).thenReturn(List.of());

        when(durationRepo.findByMinutes(60)).thenReturn(Optional.of(baseOpt));
        when(pricingRepo.findByPitchIdAndDurationOptionIdAndActiveTrue(1L, 5L)).thenReturn(Optional.of(pr));

        // reservationRepo.save: id set edip geri dönelim
        when(reservationRepo.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setId(123L);
            return r;
        });

        // request: 120 dk => 2x fiyat
        var start = ZonedDateTime.of(2030, 1, 1, 10, 0, 0, 0, ZoneId.of("Europe/Istanbul")).toInstant();
        var req = new ReservationCreateRequest(
                1L,
                start,
                120,
                PaymentMethod.CARD,
                List.of(
                        new ReservationPlayerAddRequest("Ali Veli", 10),
                        new ReservationPlayerAddRequest("Mehmet Kaya", 7)
                ),
                true
        );

        Reservation saved = svc.create(7L, req);

        assertNotNull(saved.getId());
        assertEquals(new BigDecimal("200"), saved.getTotalPrice());
        assertEquals(ReservationStatus.CONFIRMED, saved.getStatus());
        assertTrue(Boolean.TRUE.equals(saved.getShuttleRequested()));

        // players kaydedildi mi?
        verify(playerRepo, times(2)).save(any(ReservationPlayer.class));

        // payment PAID mi?
        ArgumentCaptor<Payment> payCap = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepo).save(payCap.capture());
        Payment pay = payCap.getValue();
        assertEquals(123L, pay.getReservationId());
        assertEquals(PaymentMethod.CARD, pay.getMethod());
        assertEquals(new BigDecimal("200"), pay.getAmount());
        assertEquals(PaymentStatus.PAID, pay.getStatus());
        assertNotNull(pay.getPaidAt());

        verify(audit, times(1)).log(eq(7L), eq("RESERVATION_CREATE"), eq("Reservation"), eq(123L), any(String.class));
    }
}
