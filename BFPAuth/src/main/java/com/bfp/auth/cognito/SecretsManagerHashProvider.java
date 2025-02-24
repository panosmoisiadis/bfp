package com.bfp.auth.cognito;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

public class SecretsManagerHashProvider extends SecretHashProvider {
    private static final Logger logger = LoggerFactory.getLogger(SecretsManagerHashProvider.class);

    private final SecretsManagerClient secretsManagerClient;

    public SecretsManagerHashProvider(SecretsManagerClient secretsManagerClient) {
        this.secretsManagerClient = secretsManagerClient;
    }

    String getUserPoolClientSecret(String userPoolClientId) {
        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(userPoolClientId)
                .build();

        GetSecretValueResponse response;
        try {
            response = secretsManagerClient.getSecretValue(request);
        } catch (ResourceNotFoundException resourceNotFoundException) {
            logger.error("UserPoolClientId {} not found in SecretsManager", userPoolClientId);
            throw new RuntimeException();
        }

        return response.secretString();
    }
}
