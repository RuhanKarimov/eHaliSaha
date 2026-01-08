package com.ornek.ehalisaha.ehalisahabackend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(
        name = "facilities",
        uniqueConstraints = {
                // ✅ aynı owner altında aynı isim (case-insensitive) tekrar eklenemez
                @UniqueConstraint(name = "uk_facilities_owner_name_key", columnNames = {"owner_user_id", "name_key"})
        },
        indexes = {
                @Index(name = "ix_facilities_owner", columnList = "owner_user_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(nullable = false, length = 120)
    private String name;

    // ✅ normalize edilmiş anahtar: trim + lower + çoklu boşluğu tek boşluk
    @Column(name = "name_key", nullable = false, length = 120)
    private String nameKey;

    @Column(length = 255)
    private String address;

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
