package com.ornek.ehalisaha.ehalisahabackend.repository;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findByFacilityIdAndUserId(Long facilityId, Long userId);
}
