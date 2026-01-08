package com.ornek.ehalisaha.ehalisahabackend.domain.enums;

public enum PaymentMethod {
    CASH,
    CARD;

    public boolean isInstantPaid() {
        return this == CARD;
    }
}
