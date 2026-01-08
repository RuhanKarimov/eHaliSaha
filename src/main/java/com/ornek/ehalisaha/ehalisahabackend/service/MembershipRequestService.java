package com.ornek.ehalisaha.ehalisahabackend.service;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Facility;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Membership;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.MembershipRequest;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.MembershipRequestStatus;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.MembershipStatus;
import com.ornek.ehalisaha.ehalisahabackend.repository.FacilityRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.MembershipRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.MembershipRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class MembershipRequestService {

    public record MembershipReqDto(Long id, Long facilityId, Long userId, MembershipRequestStatus status) {}

    private final MembershipRequestRepository reqRepo;
    private final FacilityRepository facilityRepo;
    private final MembershipRepository membershipRepo;
    private final AuditService audit;

    public MembershipRequestService(MembershipRequestRepository reqRepo,
                                    FacilityRepository facilityRepo,
                                    MembershipRepository membershipRepo,
                                    AuditService audit) {
        this.reqRepo = reqRepo;
        this.facilityRepo = facilityRepo;
        this.membershipRepo = membershipRepo;
        this.audit = audit;
    }

    public List<MembershipReqDto> listForOwner(Long ownerUserId) {
        return reqRepo.findAllPendingForOwner(ownerUserId)
                .stream()
                .map(r -> new MembershipReqDto(r.getId(), r.getFacilityId(), r.getUserId(), r.getStatus()))
                .toList();
    }

    @Transactional
    public Membership approve(Long ownerUserId, Long requestId) {
        MembershipRequest r = reqRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        Facility f = facilityRepo.findById(r.getFacilityId())
                .orElseThrow(() -> new IllegalStateException("Facility not found: " + r.getFacilityId()));

        if (!f.getOwnerUserId().equals(ownerUserId)) {
            throw new SecurityException("Not your facility");
        }

        // request zaten sonuçlandıysa tekrar işleme
        if (r.getStatus() != MembershipRequestStatus.PENDING) {
            throw new IllegalStateException("Request already decided: " + r.getStatus());
        }

        // membership upsert
        Membership m = membershipRepo.findByFacilityIdAndUserId(r.getFacilityId(), r.getUserId())
                .orElseGet(() -> {
                    Membership x = new Membership();
                    x.setFacilityId(r.getFacilityId());
                    x.setUserId(r.getUserId());
                    return x;
                });

        m.setStatus(MembershipStatus.ACTIVE);
        Membership savedMembership = membershipRepo.save(m);

        r.setStatus(MembershipRequestStatus.APPROVED);
        r.setDecidedAt(Instant.now());
        r.setDecidedBy(ownerUserId);
        reqRepo.save(r);

        audit.log(ownerUserId, "MEMBERSHIP_APPROVE", "MembershipRequest", requestId,
                "facilityId=" + r.getFacilityId() + ", userId=" + r.getUserId());

        return savedMembership;
    }

    @Transactional
    public MembershipRequest reject(Long ownerUserId, Long requestId) {
        MembershipRequest r = reqRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        Facility f = facilityRepo.findById(r.getFacilityId())
                .orElseThrow(() -> new IllegalStateException("Facility not found: " + r.getFacilityId()));

        if (!f.getOwnerUserId().equals(ownerUserId)) {
            throw new SecurityException("Not your facility");
        }

        if (r.getStatus() != MembershipRequestStatus.PENDING) {
            throw new IllegalStateException("Request already decided: " + r.getStatus());
        }

        r.setStatus(MembershipRequestStatus.REJECTED);
        r.setDecidedAt(Instant.now());
        r.setDecidedBy(ownerUserId);
        MembershipRequest saved = reqRepo.save(r);

        audit.log(ownerUserId, "MEMBERSHIP_REJECT", "MembershipRequest", requestId,
                "facilityId=" + r.getFacilityId() + ", userId=" + r.getUserId());

        return saved;
    }
}
