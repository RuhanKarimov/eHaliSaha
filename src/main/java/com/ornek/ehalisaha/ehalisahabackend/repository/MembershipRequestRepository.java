package com.ornek.ehalisaha.ehalisahabackend.repository;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.MembershipRequest;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.MembershipRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MembershipRequestRepository extends JpaRepository<MembershipRequest, Long> {

    Optional<MembershipRequest> findByFacilityIdAndUserId(Long facilityId, Long userId);

    @Query("""
      select r from MembershipRequest r
      join Facility f on f.id = r.facilityId
      where f.ownerUserId = :ownerUserId
        and r.status = com.ornek.ehalisaha.ehalisahabackend.domain.enums.MembershipRequestStatus.PENDING
    """)
    List<MembershipRequest> findAllPendingForOwner(Long ownerUserId);

    List<MembershipRequest> findByFacilityIdAndStatus(Long facilityId, MembershipRequestStatus status);

    List<MembershipRequest> findByFacilityIdIn(List<Long> facilityIds);

    List<MembershipRequest> findByFacilityIdInAndStatus(List<Long> facilityIds, MembershipRequestStatus status);

}
