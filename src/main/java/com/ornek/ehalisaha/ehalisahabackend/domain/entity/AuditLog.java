package com.ornek.ehalisaha.ehalisahabackend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter
@Entity
@Table(name="audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="actor_user_id")
    private Long actorUserId;

    @Column(nullable=false, length=80)
    private String action;

    @Column(name="entity_type", length=40)
    private String entityType;

    @Column(name="entity_id")
    private Long entityId;

    @Column(columnDefinition = "text")
    private String detail;

    @Column(name="created_at", insertable=false, updatable=false)
    private Instant createdAt;
}
