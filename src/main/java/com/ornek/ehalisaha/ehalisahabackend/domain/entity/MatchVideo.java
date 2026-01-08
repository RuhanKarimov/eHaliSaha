package com.ornek.ehalisaha.ehalisahabackend.domain.entity;

import com.ornek.ehalisaha.ehalisahabackend.domain.enums.VideoStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "match_videos",
        uniqueConstraints = @UniqueConstraint(name="uk_video_reservation", columnNames = {"reservation_id"}),
        indexes = {
                @Index(name = "ix_video_status", columnList = "status")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class MatchVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="reservation_id", nullable = false)
    private Long reservationId;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false, length = 20)
    private VideoStatus status = VideoStatus.PUBLISHED;

    @Column(name="storage_url", nullable = false, length = 255)
    private String storageUrl;

    @Column(name="published_at")
    private Instant publishedAt;

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = VideoStatus.PUBLISHED;
    }
}
