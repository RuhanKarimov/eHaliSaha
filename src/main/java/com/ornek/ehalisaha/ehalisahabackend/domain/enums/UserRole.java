package com.ornek.ehalisaha.ehalisahabackend.domain.enums;

public enum UserRole {
    OWNER,
    MEMBER,
    ADMIN;

    public String asAuthority() {
        return "ROLE_" + this.name();
    }

    public static UserRole fromDb(String s) {
        if (s == null) return null;
        s = s.trim().toUpperCase();
        if (s.startsWith("ROLE_")) s = s.substring("ROLE_".length());
        return UserRole.valueOf(s);
    }
}
