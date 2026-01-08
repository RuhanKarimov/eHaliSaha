package com.ornek.ehalisaha.ehalisahabackend.repository;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.ReservationPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationPlayerRepository extends JpaRepository<ReservationPlayer, Long> {
    List<ReservationPlayer> findByReservationId(Long reservationId);
    List<ReservationPlayer> findByReservationIdIn(List<Long> reservationIds);

}
