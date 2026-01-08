package com.ornek.ehalisaha.ehalisahabackend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "facility_slots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_facility_slot",
                columnNames = {"facility_id", "start_minute", "duration_minutes"}
        ),
        indexes = {
                @Index(name = "ix_slots_facility", columnList = "facility_id"),
                @Index(name = "ix_slots_active", columnList = "active")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class FacilitySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="facility_id", nullable = false)
    private Long facilityId;

    @Column(name="start_minute", nullable = false)
    private Integer startMinute; // 17:00 => 1020

    @Column(name="duration_minutes", nullable = false)
    private Integer durationMinutes; // 60

    @Column(nullable = false)
    private Boolean active = true;

    @PrePersist
    void prePersist() {
        if (active == null) active = true;
    }
}
