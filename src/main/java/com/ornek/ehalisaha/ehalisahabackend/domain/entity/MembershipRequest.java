package com.ornek.ehalisaha.ehalisahabackend.domain.entity;

import com.ornek.ehalisaha.ehalisahabackend.domain.enums.MembershipRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "membership_requests",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_membership_req_facility_user",
                columnNames = {"facility_id", "user_id"}
        ),
        indexes = {
                @Index(name = "ix_mreq_facility", columnList = "facility_id"),
                @Index(name = "ix_mreq_user", columnList = "user_id"),
                @Index(name = "ix_mreq_status", columnList = "status")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class MembershipRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="facility_id", nullable = false)
    private Long facilityId;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false, length = 20)
    private MembershipRequestStatus status = MembershipRequestStatus.PENDING;

    @Column(name="requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name="decided_at")
    private Instant decidedAt;

    @Column(name="decided_by")
    private Long decidedBy;

    @PrePersist
    void prePersist() {
        if (requestedAt == null) requestedAt = Instant.now();
        if (status == null) status = MembershipRequestStatus.PENDING;
    }
}
