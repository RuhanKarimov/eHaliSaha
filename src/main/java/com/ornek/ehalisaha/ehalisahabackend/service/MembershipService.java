package com.ornek.ehalisaha.ehalisahabackend.service;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Membership;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.MembershipRequest;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.MembershipRequestStatus;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.MembershipStatus;
import com.ornek.ehalisaha.ehalisahabackend.repository.FacilityRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.MembershipRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.MembershipRequestRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepo;
    private final MembershipRequestRepository reqRepo;
    private final FacilityRepository facilityRepo;

    public MembershipService(MembershipRepository membershipRepo,
                             MembershipRequestRepository reqRepo,
                             FacilityRepository facilityRepo) {
        this.membershipRepo = membershipRepo;
        this.reqRepo = reqRepo;
        this.facilityRepo = facilityRepo;
    }

    // MEMBER: create request
    @Transactional
    public MembershipRequest createRequest(Long userId, Long facilityId) {
        facilityRepo.findById(facilityId).orElseThrow(() -> new IllegalStateException("Facility not found"));

        // Aynı facility için tekrar istek atmayı engelle
        reqRepo.findByFacilityIdAndUserId(facilityId, userId).ifPresent(r -> {
            throw new IllegalStateException("Request already exists");
        });

        // Zaten aktif üyelik varsa da engelle
        membershipRepo.findByFacilityIdAndUserId(facilityId, userId).ifPresent(m -> {
            if (m.getStatus() == MembershipStatus.ACTIVE) {
                throw new IllegalStateException("Membership already active");
            }
        });

        MembershipRequest req = MembershipRequest.builder()
                .userId(userId)
                .facilityId(facilityId)
                .status(MembershipRequestStatus.PENDING)
                .build();
        return reqRepo.save(req);
    }

    // OWNER: approve
    @Transactional
    public MembershipRequest approve(Long ownerUserId, Long requestId) {
        MembershipRequest req = reqRepo.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Request not found"));

        // membership oluştur / aktif et
        Membership m = membershipRepo.findByFacilityIdAndUserId(req.getFacilityId(), req.getUserId())
                .orElseGet(() -> Membership.builder()
                        .facilityId(req.getFacilityId())
                        .userId(req.getUserId())
                        .status(MembershipStatus.ACTIVE)
                        .build());

        m.setStatus(MembershipStatus.ACTIVE);
        membershipRepo.save(m);

        req.setStatus(MembershipRequestStatus.APPROVED);
        return reqRepo.save(req);
    }

    // OWNER: reject
    @Transactional
    public MembershipRequest reject(Long ownerUserId, Long requestId) {
        MembershipRequest req = reqRepo.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Request not found"));
        req.setStatus(MembershipRequestStatus.REJECTED);
        return reqRepo.save(req);
    }

    // MEMBER: facility bazlı üyelik durumunu görür (UI senkronu için)
    @Transactional(Transactional.TxType.SUPPORTS)
    public MemberFacilityMembershipStatus getMemberFacilityStatus(Long userId, Long facilityId) {
        MembershipStatus ms = null;
        Long membershipId = null;

        var m = membershipRepo.findByFacilityIdAndUserId(facilityId, userId).orElse(null);
        if (m != null) {
            ms = m.getStatus();
            membershipId = m.getId();
        }

        MembershipRequestStatus rs = reqRepo.findByFacilityIdAndUserId(facilityId, userId)
                .map(MembershipRequest::getStatus)
                .orElse(null);

        return new MemberFacilityMembershipStatus(facilityId, ms, membershipId, rs);
    }

    public record MemberFacilityMembershipStatus(
            Long facilityId,
            MembershipStatus membershipStatus,
            Long membershipId,
            MembershipRequestStatus requestStatus
    ) {}

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<MembershipRequest> listRequestsForOwner(Long ownerUserId) {
        List<Long> facilityIds = facilityRepo.findIdsByOwnerUserId(ownerUserId);
        if (facilityIds == null || facilityIds.isEmpty()) return Collections.emptyList();

        // sadece bekleyenler gelsin (owner paneli mantığı)
        return reqRepo.findByFacilityIdInAndStatus(facilityIds, MembershipRequestStatus.PENDING);
    }

}
