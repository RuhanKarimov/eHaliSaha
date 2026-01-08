package com.ornek.ehalisaha.ehalisahabackend.domain.entity;

import com.ornek.ehalisaha.ehalisahabackend.domain.enums.PaymentMethod;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = @UniqueConstraint(name="uk_payment_reservation", columnNames = {"reservation_id"}),
        indexes = {
                @Index(name = "ix_pay_status", columnList = "status")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="reservation_id", nullable = false)
    private Long reservationId;

    @Enumerated(EnumType.STRING)
    @Column(name="method", nullable = false, length = 10)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name="amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name="currency", nullable = false, length = 10)
    private String currency = "TRY";

    @Column(name="provider_ref", length = 120)
    private String providerRef;

    @Column(name="paid_at")
    private Instant paidAt;

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (currency == null) currency = "TRY";
        if (amount == null) amount = BigDecimal.ZERO;
    }
}
