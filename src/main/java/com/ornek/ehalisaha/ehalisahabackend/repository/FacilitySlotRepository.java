package com.ornek.ehalisaha.ehalisahabackend.repository;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.FacilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface FacilitySlotRepository extends JpaRepository<FacilitySlot, Long> {

    List<FacilitySlot> findByFacilityIdAndActiveTrueOrderByStartMinuteAsc(Long facilityId);
    List<FacilitySlot> findByFacilityIdOrderByStartMinuteAsc(Long facilityId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    void deleteByFacilityId(Long facilityId);
}
