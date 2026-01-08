package com.ornek.ehalisaha.ehalisahabackend.domain.enums;

public enum ReservationStatus {
    CREATED,
    CONFIRMED,
    CANCELLED,
    COMPLETED;

    public boolean isFinal() {
        return this == CANCELLED || this == COMPLETED;
    }
}
