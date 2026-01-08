package com.ornek.ehalisaha.ehalisahabackend.repository;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.DurationOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DurationOptionRepository extends JpaRepository<DurationOption, Long> {
    Optional<DurationOption> findByMinutes(Integer minutes);

    @Modifying
    @Query("delete from DurationOption d where d.minutes <> ?1")
    void deleteByMinutesNot(Integer keepMinutes);
}
