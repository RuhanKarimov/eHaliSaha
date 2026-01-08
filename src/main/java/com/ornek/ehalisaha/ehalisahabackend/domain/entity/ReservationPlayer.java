package com.ornek.ehalisaha.ehalisahabackend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "reservation_players",
        indexes = {
                @Index(name = "ix_rp_reservation", columnList = "reservation_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ReservationPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="reservation_id", nullable = false)
    private Long reservationId;

    @Column(name="full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name="jersey_no")
    private Integer jerseyNo;

    @Column(name="paid", nullable = false)
    private boolean paid = false;
}
