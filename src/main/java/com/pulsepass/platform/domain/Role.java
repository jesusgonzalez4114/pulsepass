package com.pulsepass.platform.domain;

public enum Role {
    USER,
    STAFF,
    ADMIN;

    /** Nombre de la authority en Spring Security (ROLE_USER, ROLE_STAFF, ROLE_ADMIN). */
    public String authority() {
        return "ROLE_" + name();
    }
}
