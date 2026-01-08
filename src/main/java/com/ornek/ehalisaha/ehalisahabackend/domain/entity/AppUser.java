package com.ornek.ehalisaha.ehalisahabackend.domain.entity;

import com.ornek.ehalisaha.ehalisahabackend.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

@Entity
@Table(
        name = "app_users",
        indexes = {
                @Index(name = "ix_app_users_username", columnList = "username")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;


    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private UserRole role;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (enabled == null) enabled = true;
        if (role == null) role = UserRole.MEMBER;
    }
}
