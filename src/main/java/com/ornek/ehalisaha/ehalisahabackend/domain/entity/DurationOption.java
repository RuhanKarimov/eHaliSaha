package com.ornek.ehalisaha.ehalisahabackend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "duration_options",
        uniqueConstraints = @UniqueConstraint(name = "uk_duration_minutes", columnNames = {"minutes"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DurationOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="minutes", nullable = false)
    private Integer minutes;

    @Column(name="label", nullable = false, length = 30)
    private String label;
}
