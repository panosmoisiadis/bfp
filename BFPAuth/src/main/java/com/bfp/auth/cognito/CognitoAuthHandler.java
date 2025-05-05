package com.bfp.auth.cognito;

import com.bfp.auth.AuthHandler;
import com.bfp.auth.token.TokenHandler;
import com.bfp.model.AuthenticateRequest;
import com.bfp.model.AuthenticateResponse;
import com.bfp.model.exceptions.UnauthorizedException;
import lombok.NonNull;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GlobalSignOutRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.RevokeTokenRequest;

import java.time.Instant;
import java.util.Map;

public class CognitoAuthHandler implements AuthHandler {
    private final CognitoUserPoolClientSecretHashProvider clientSecretHashProvider;

    private final CognitoIdentityProviderClient cognitoIdentityProviderClient;
    private final String userPoolId, userPoolClientId;

    private final TokenHandler tokenHandler;

    public CognitoAuthHandler(CognitoUserPoolClientSecretHashProvider clientSecretHashProvider,
                              CognitoIdentityProviderClient cognitoIdentityProviderClient, String clientId, String userPoolId,
                              TokenHandler tokenHandler) {
        this.clientSecretHashProvider = clientSecretHashProvider;
        this.cognitoIdentityProviderClient = cognitoIdentityProviderClient;
        this.userPoolId = userPoolId;
        this.userPoolClientId = clientId;
        this.tokenHandler = tokenHandler;
    }

    @Override
    public AuthenticateResponse authenticate(@NonNull final AuthenticateRequest authenticateRequest) {
        String secretHash = clientSecretHashProvider.getClientSecretHash(userPoolClientId, authenticateRequest.getUsername());

        Map<String, String> authParameters = Map.of(
                "USERNAME", authenticateRequest.getUsername(),
                "PASSWORD", authenticateRequest.getPassword(),
                "SECRET_HASH", secretHash
        );

        AdminInitiateAuthRequest request = AdminInitiateAuthRequest.builder()
                .userPoolId(userPoolId)
                .clientId(userPoolClientId)
                .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                .authParameters(authParameters)
                .build();

        AdminInitiateAuthResponse cognitoResponse;

        try {
            cognitoResponse = cognitoIdentityProviderClient.adminInitiateAuth(request);
        } catch (NotAuthorizedException notAuthorizedException) {
            throw new UnauthorizedException();
        }

        String accessToken = cognitoResponse.authenticationResult().accessToken();

        if (accessToken == null || accessToken.isBlank()) {
            throw new UnauthorizedException();
        }

        AuthenticateResponse response = AuthenticateResponse.builder()
                .accessToken(accessToken)
                .build();

        return response;
    }

    @Override
    public void signOut(String accessToken, String accessTokenId, Instant expiry) {
        cognitoIdentityProviderClient.globalSignOut(GlobalSignOutRequest.builder()
                .accessToken(accessToken)
                .build());
        tokenHandler.disableAccessToken(accessTokenId, expiry);
    }
}
