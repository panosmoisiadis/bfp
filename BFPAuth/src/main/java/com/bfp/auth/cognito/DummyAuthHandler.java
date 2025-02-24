package com.bfp.auth.cognito;

import com.bfp.auth.AuthHandler;
import com.bfp.model.AuthenticateRequest;
import com.bfp.model.AuthenticateResponse;

public class DummyAuthHandler implements AuthHandler {
    @Override
    public AuthenticateResponse authenticate(AuthenticateRequest AuthenticateRequest) {
        AuthenticateResponse response = AuthenticateResponse.builder()
                .accessToken("dummy_access_token")
                .build();
        return response;
    }

    @Override
    public void signOut(String accessToken) {

    }
}
