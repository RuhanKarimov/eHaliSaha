package com.ornek.ehalisaha.ehalisahabackend.domain.entity;

import com.ornek.ehalisaha.ehalisahabackend.domain.enums.MembershipStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "memberships",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_membership_facility_user",
                columnNames = {"facility_id", "user_id"}
        ),
        indexes = {
                @Index(name = "ix_membership_facility", columnList = "facility_id"),
                @Index(name = "ix_membership_user", columnList = "user_id"),
                @Index(name = "ix_membership_status", columnList = "status")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="facility_id", nullable = false)
    private Long facilityId;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false, length = 20)
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @Column(name="started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @PrePersist
    void prePersist() {
        if (startedAt == null) startedAt = Instant.now();
        if (status == null) status = MembershipStatus.ACTIVE;
    }
}
