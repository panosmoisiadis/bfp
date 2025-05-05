package com.bfp.auth.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamoDbBean
public class AccessTokenDO {
    private String tokenId;
    private AccessTokenStatus status;
    private Instant expiryDate;

    @DynamoDbPartitionKey
    public String getTokenId() {
        return tokenId;
    }

    boolean isEnabled() {
        return AccessTokenStatus.ENABLED.equals(status);
    }
}
