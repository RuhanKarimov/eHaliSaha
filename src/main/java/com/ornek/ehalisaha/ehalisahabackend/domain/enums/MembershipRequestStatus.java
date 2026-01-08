package com.ornek.ehalisaha.ehalisahabackend.domain.enums;

public enum MembershipRequestStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public boolean isFinal() {
        return this == APPROVED || this == REJECTED;
    }
}
