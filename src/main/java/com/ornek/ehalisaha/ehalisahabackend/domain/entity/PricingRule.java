package com.ornek.ehalisaha.ehalisahabackend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "pricing_rules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pricing_pitch_duration",
                columnNames = {"pitch_id", "duration_option_id"}
        ),
        indexes = {
                @Index(name = "ix_pricing_pitch", columnList = "pitch_id"),
                @Index(name = "ix_pricing_duration", columnList = "duration_option_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="pitch_id", nullable = false)
    private Long pitchId;

    @Column(name="duration_option_id", nullable = false)
    private Long durationOptionId;

    @Column(name="price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name="currency", nullable = false, length = 10)
    private String currency = "TRY";

    @Column(name="active", nullable = false)
    private Boolean active = true;

    @PrePersist
    void prePersist() {
        if (currency == null) currency = "TRY";
        if (active == null) active = true;
        if (price == null) price = BigDecimal.ZERO;
    }
}
