package com.ornek.ehalisaha.ehalisahabackend.repository;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FacilityRepository extends JpaRepository<Facility, Long> {
    List<Facility> findByOwnerUserId(Long ownerUserId);

    // NOTE: Facility entity field is 'ownerUserId' (column owner_user_id)
    // We want only facility ids for a given owner, so use explicit JPQL.
    @Query("select f.id from Facility f where f.ownerUserId = :ownerUserId")
    List<Long> findIdsByOwnerUserId(@Param("ownerUserId") Long ownerUserId);

    boolean existsByOwnerUserIdAndNameKey(Long ownerUserId, String nameKey);

}
