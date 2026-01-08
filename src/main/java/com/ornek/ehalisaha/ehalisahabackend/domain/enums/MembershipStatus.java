package com.ornek.ehalisaha.ehalisahabackend.domain.enums;

public enum MembershipStatus {
    ACTIVE,
    SUSPENDED,
    CANCELLED;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
