package com.ornek.ehalisaha.ehalisahabackend.repository;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Reservation;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByPitchIdInAndStartTimeBetween(List<Long> pitchIds, Instant from, Instant to);

    List<Reservation> findByPitchIdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long pitchId, Instant end, Instant start
    );

    @Query("""
        select r from Reservation r
        where r.pitchId = :pitchId
          and r.status <> :excludedStatus
          and r.startTime < :end
          and r.endTime > :start
    """)
    List<Reservation> findOverlappingForPitch(
            @Param("pitchId") Long pitchId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("excludedStatus") ReservationStatus excludedStatus
    );

    @Query("""
        select r from Reservation r
        where r.pitchId in :pitchIds
          and r.status <> :excludedStatus
          and r.startTime < :end
          and r.endTime > :start
    """)
    List<Reservation> findOverlappingForPitches(
            @Param("pitchIds") List<Long> pitchIds,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("excludedStatus") ReservationStatus excludedStatus
    );

    List<Reservation> findByStatusAndEndTimeBefore(ReservationStatus status, Instant t);

    @Query("""
        select r.id
        from Reservation r
        join Membership m on m.id = r.membershipId
        where m.userId = :userId
    """)
    List<Long> findReservationIdsByUserId(@Param("userId") Long userId);

    @Query("""
  select coalesce(max(r.id), 0)
  from Reservation r, Pitch p, Facility f
  where r.pitchId = p.id
    and p.facilityId = f.id
    and f.ownerUserId = :ownerId
    and r.status <> :excludedStatus
""")
    Long ownerMaxReservationId(@Param("ownerId") Long ownerId,
                               @Param("excludedStatus") ReservationStatus excludedStatus);

    @Query("""
  select count(r)
  from Reservation r, Pitch p, Facility f
  where r.pitchId = p.id
    and p.facilityId = f.id
    and f.ownerUserId = :ownerId
    and r.id > :afterId
    and r.status <> :excludedStatus
""")
    long ownerCountReservationsAfterId(@Param("ownerId") Long ownerId,
                                       @Param("afterId") Long afterId,
                                       @Param("excludedStatus") ReservationStatus excludedStatus);


}
