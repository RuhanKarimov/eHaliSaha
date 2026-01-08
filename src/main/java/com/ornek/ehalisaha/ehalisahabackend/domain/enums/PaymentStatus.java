package com.ornek.ehalisaha.ehalisahabackend.domain.enums;

public enum PaymentStatus {
    INIT,
    PAID,
    FAILED,
    CANCELLED;

    public boolean isPaid() {
        return this == PAID;
    }
}
