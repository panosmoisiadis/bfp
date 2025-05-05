package com.bfp.auth.token;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TokenHandler {
    private final AccessTokenDAO accessTokenDAO;

    @Autowired
    public TokenHandler(AccessTokenDAO accessTokenDAO) {
        this.accessTokenDAO = accessTokenDAO;
    }

    public boolean isAccessTokenValid(final String accessTokenId) {
        AccessTokenDO accessTokenDO = accessTokenDAO.findById(accessTokenId);
        return accessTokenDO == null || accessTokenDO.isEnabled();
    }

    public void disableAccessToken(final String accessTokenId, final Instant expiryTime) {
        AccessTokenDO revokedToken = AccessTokenDO.builder()
                .tokenId(accessTokenId)
                .status(AccessTokenStatus.REVOKED)
                .expiryDate(expiryTime)
                .build();
        accessTokenDAO.save(revokedToken);
    }
}
