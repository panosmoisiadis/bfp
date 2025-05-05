package com.bfp.auth.token;

public enum AccessTokenStatus {
    ENABLED,
    REVOKED;

    public AccessTokenStatus fromString(String status) {
        return AccessTokenStatus.valueOf(status.toUpperCase());
    }
}
