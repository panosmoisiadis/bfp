package com.bfp.auth.cognito;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public abstract class SecretHashProvider implements CognitoUserPoolClientSecretHashProvider {
    private static final Logger logger = LoggerFactory.getLogger(SecretsManagerHashProvider.class);

    @Override
    public String getClientSecretHash(String userPoolClientId, String userName) {
        String userPoolClientSecret = getUserPoolClientSecret(userPoolClientId);
        final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

        SecretKeySpec signingKey = new SecretKeySpec(
                userPoolClientSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256_ALGORITHM);
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(signingKey);
            mac.update(userName.getBytes(StandardCharsets.UTF_8));
            byte[] rawHmac = mac.doFinal(userPoolClientId.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            logger.error("Error while calculating secret hash with UserPoolClientId {} and Username {}", userPoolClientId, userName);
            throw new RuntimeException();
        }
    }

    abstract String getUserPoolClientSecret(String userPoolClientId);
}
