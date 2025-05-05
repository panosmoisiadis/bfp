package com.bfp.auth;

import com.bfp.model.AuthenticateRequest;
import com.bfp.model.AuthenticateResponse;

import java.time.Instant;

public interface AuthHandler {
    AuthenticateResponse authenticate(AuthenticateRequest AuthenticateRequest);
    void signOut(String accessToken, String accessTokenId, Instant accessTokenExpiry);
}
