package com.bfp.cdk;

import lombok.Builder;
import software.amazon.awscdk.StackProps;

@Builder
public record BFPServiceStackProps(boolean shouldCreateEcsService) implements StackProps {
}
