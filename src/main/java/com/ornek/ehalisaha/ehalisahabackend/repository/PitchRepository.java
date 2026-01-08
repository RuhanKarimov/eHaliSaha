package com.ornek.ehalisaha.ehalisahabackend.repository;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Pitch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PitchRepository extends JpaRepository<Pitch, Long> {
    List<Pitch> findByFacilityId(Long facilityId);
    List<Pitch> findByFacilityIdIn(List<Long> facilityIds);
    List<Pitch> findByFacilityIdAndActiveTrueOrderByIdAsc(Long facilityId);

    boolean existsByFacilityIdAndNameKey(Long facilityId, String nameKey);

}
