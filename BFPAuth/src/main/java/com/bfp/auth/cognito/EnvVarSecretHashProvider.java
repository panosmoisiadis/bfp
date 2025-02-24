package com.bfp.auth.cognito;

public class EnvVarSecretHashProvider extends SecretHashProvider {
    @Override
    String getUserPoolClientSecret(String userPoolClientId) {
        return System.getenv("CLIENT_SECRET");
    }
}
