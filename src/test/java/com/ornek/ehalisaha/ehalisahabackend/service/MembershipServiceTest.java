package com.ornek.ehalisaha.ehalisahabackend.service;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Membership;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.MembershipRequest;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.MembershipRequestStatus;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.MembershipStatus;
import com.ornek.ehalisaha.ehalisahabackend.repository.FacilityRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.MembershipRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.MembershipRequestRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MembershipService unit testleri (Spring yok).
 * Gerçekçi senaryolar: tekrar istek engeli, aktif üyelik engeli, approve akışı, owner pending liste.
 */
class MembershipServiceTest {

    @Test
    void createRequest_shouldThrow_whenRequestAlreadyExists() {
        MembershipRepository membershipRepo = mock(MembershipRepository.class);
        MembershipRequestRepository reqRepo = mock(MembershipRequestRepository.class);
        FacilityRepository facilityRepo = mock(FacilityRepository.class);

        MembershipService svc = new MembershipService(membershipRepo, reqRepo, facilityRepo);

        when(facilityRepo.findById(10L)).thenReturn(Optional.of(new com.ornek.ehalisaha.ehalisahabackend.domain.entity.Facility()));
        when(reqRepo.findByFacilityIdAndUserId(10L, 7L)).thenReturn(Optional.of(MembershipRequest.builder()
                .id(1L)
                .facilityId(10L)
                .userId(7L)
                .status(MembershipRequestStatus.PENDING)
                .build()));

        assertThrows(IllegalStateException.class, () -> svc.createRequest(7L, 10L));
        verify(reqRepo, never()).save(any());
    }

    @Test
    void createRequest_shouldThrow_whenMembershipAlreadyActive() {
        MembershipRepository membershipRepo = mock(MembershipRepository.class);
        MembershipRequestRepository reqRepo = mock(MembershipRequestRepository.class);
        FacilityRepository facilityRepo = mock(FacilityRepository.class);

        MembershipService svc = new MembershipService(membershipRepo, reqRepo, facilityRepo);

        when(facilityRepo.findById(10L)).thenReturn(Optional.of(new com.ornek.ehalisaha.ehalisahabackend.domain.entity.Facility()));
        when(reqRepo.findByFacilityIdAndUserId(10L, 7L)).thenReturn(Optional.empty());
        when(membershipRepo.findByFacilityIdAndUserId(10L, 7L)).thenReturn(Optional.of(Membership.builder()
                .id(99L)
                .facilityId(10L)
                .userId(7L)
                .status(MembershipStatus.ACTIVE)
                .build()));

        assertThrows(IllegalStateException.class, () -> svc.createRequest(7L, 10L));
        verify(reqRepo, never()).save(any());
    }

    @Test
    void approve_shouldActivateMembership_andMarkRequestApproved() {
        MembershipRepository membershipRepo = mock(MembershipRepository.class);
        MembershipRequestRepository reqRepo = mock(MembershipRequestRepository.class);
        FacilityRepository facilityRepo = mock(FacilityRepository.class);

        MembershipService svc = new MembershipService(membershipRepo, reqRepo, facilityRepo);

        MembershipRequest req = MembershipRequest.builder()
                .id(5L)
                .facilityId(10L)
                .userId(7L)
                .status(MembershipRequestStatus.PENDING)
                .build();

        when(reqRepo.findById(5L)).thenReturn(Optional.of(req));
        when(membershipRepo.findByFacilityIdAndUserId(10L, 7L)).thenReturn(Optional.empty());

        when(membershipRepo.save(any(Membership.class))).thenAnswer(inv -> {
            Membership m = inv.getArgument(0);
            if (m.getId() == null) m.setId(77L);
            return m;
        });

        when(reqRepo.save(any(MembershipRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        MembershipRequest out = svc.approve(1L, 5L);
        assertEquals(MembershipRequestStatus.APPROVED, out.getStatus());

        verify(membershipRepo).save(argThat(m ->
                m.getFacilityId().equals(10L)
                        && m.getUserId().equals(7L)
                        && m.getStatus() == MembershipStatus.ACTIVE
        ));
    }

    @Test
    void listRequestsForOwner_shouldReturnEmpty_whenOwnerHasNoFacilities() {
        MembershipRepository membershipRepo = mock(MembershipRepository.class);
        MembershipRequestRepository reqRepo = mock(MembershipRequestRepository.class);
        FacilityRepository facilityRepo = mock(FacilityRepository.class);

        MembershipService svc = new MembershipService(membershipRepo, reqRepo, facilityRepo);

        when(facilityRepo.findIdsByOwnerUserId(1L)).thenReturn(List.of());

        var out = svc.listRequestsForOwner(1L);
        assertTrue(out.isEmpty());
        verifyNoInteractions(reqRepo);
    }
}
