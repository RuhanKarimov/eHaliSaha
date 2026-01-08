package com.ornek.ehalisaha.ehalisahabackend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(
        name = "pitches",
        uniqueConstraints = {
                // ✅ aynı facility içinde aynı isim (case-insensitive) tekrar eklenemez
                @UniqueConstraint(name = "uk_pitches_facility_name_key", columnNames = {"facility_id", "name_key"})
        },
        indexes = {
                @Index(name = "ix_pitches_facility", columnList = "facility_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Pitch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="facility_id", nullable = false)
    private Long facilityId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "name_key", nullable = false, length = 120)
    private String nameKey;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    @PreUpdate
    void normalize() {
        if (name != null) name = name.trim();
        this.nameKey = normalizeKey(this.name);

        if (createdAt == null) createdAt = Instant.now();
        if (active == null) active = true;
    }

    private static String normalizeKey(String s) {
        if (s == null) return "";
        return s.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }
}
