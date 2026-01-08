package com.ornek.ehalisaha.ehalisahabackend.repository;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.MatchVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MatchVideoRepository extends JpaRepository<MatchVideo, Long> {
    Optional<MatchVideo> findByReservationId(Long reservationId);
    List<MatchVideo> findByReservationIdIn(Collection<Long> reservationIds);
}
