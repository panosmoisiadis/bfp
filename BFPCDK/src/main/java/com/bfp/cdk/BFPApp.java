package com.bfp.cdk;

import software.amazon.awscdk.App;

import java.util.Objects;

public class BFPApp {
    public static void main(String[] args) {
        App app = new App();
        String stage = (String) app.getNode().tryGetContext("stage");
        Object createEcsService = Objects.requireNonNullElse(app.getNode().tryGetContext("createEcsService"), false);
        boolean createEcsServiceBool = Boolean.parseBoolean(createEcsService.toString());
        Stage stageEnum = Stage.fromString(stage);

        BFPServiceStackProps props = BFPServiceStackProps.builder()
                .shouldCreateEcsService(createEcsServiceBool)
                .build();

        BFPServiceStack serviceStack = new BFPServiceStack(app, "BFPService", props, stageEnum);
        BFPServicePipeline devOpsStack = new BFPServicePipeline(app, "BFPDevOps");
        app.synth();
    }
}
