package com.ornek.ehalisaha.ehalisahabackend.domain.entity;

import com.ornek.ehalisaha.ehalisahabackend.domain.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "reservations",
        indexes = {
                @Index(name = "ix_res_pitch", columnList = "pitch_id"),
                @Index(name = "ix_res_membership", columnList = "membership_id"),
                @Index(name = "ix_res_start", columnList = "start_time"),
                @Index(name = "ix_res_end", columnList = "end_time")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="pitch_id", nullable = false)
    private Long pitchId;

    @Column(name="membership_id", nullable = false)
    private Long membershipId;

    @Column(name="start_time", nullable = false)
    private Instant startTime;

    @Column(name="end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.CREATED;

    @Column(name="total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(name="shuttle_requested", nullable = false)
    private Boolean shuttleRequested = false;

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = ReservationStatus.CREATED;
        if (totalPrice == null) totalPrice = BigDecimal.ZERO;
        if (shuttleRequested == null) shuttleRequested = false;
    }
}
