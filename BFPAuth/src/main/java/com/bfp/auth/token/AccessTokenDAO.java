package com.bfp.auth.token;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

@Component
class AccessTokenDAO {
    private final DynamoDbTable<AccessTokenDO> tokenTable;

    @Autowired
    public AccessTokenDAO(DynamoDbTable<AccessTokenDO> tokenTable) {
        this.tokenTable = tokenTable;
    }

    public AccessTokenDO findById(String tokenId) {
        AccessTokenDO accessTokenDO;
        try {
            accessTokenDO = tokenTable.getItem(GetItemEnhancedRequest.builder()
                    .key(Key.builder()
                            .partitionValue(tokenId)
                            .build())
                    .build()
            );
        } catch (ResourceNotFoundException resourceNotFoundException) {
            return null;
        }
        return accessTokenDO;
    }

    public void save(AccessTokenDO accessTokenDO) {
        tokenTable.putItem(accessTokenDO);
    }
}
