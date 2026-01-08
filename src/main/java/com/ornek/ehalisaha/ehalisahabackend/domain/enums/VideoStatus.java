package com.ornek.ehalisaha.ehalisahabackend.domain.enums;

public enum VideoStatus {
    PUBLISHED,
    HIDDEN,
    PROCESSING;

    public boolean isVisibleToMember() {
        return this == PUBLISHED;
    }
}
