package com.bfp.cdk;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariable;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariableType;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codebuild.ComputeType;
import software.amazon.awscdk.services.codebuild.LinuxArmLambdaBuildImage;
import software.amazon.awscdk.services.codebuild.PipelineProject;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.ExecutionMode;
import software.amazon.awscdk.services.codepipeline.GitConfiguration;
import software.amazon.awscdk.services.codepipeline.GitPushFilter;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.PipelineType;
import software.amazon.awscdk.services.codepipeline.ProviderType;
import software.amazon.awscdk.services.codepipeline.StageProps;
import software.amazon.awscdk.services.codepipeline.TriggerProps;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildActionType;
import software.amazon.awscdk.services.codepipeline.actions.CodeStarConnectionsSourceAction;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.s3.Bucket;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class BFPServicePipeline extends Stack {
    static String CODESTAR_CONNECTION_ARN = "arn:aws:codeconnections:us-west-2:891377256793:connection/15bc2a4c-dafa-44ce-b797-cbf10226132b";

    public BFPServicePipeline(Construct parent, String id) {
        super(parent, id);
        createPipeline();
    }

    Pipeline createPipeline() {
        Artifact sourceArtifact = Artifact.artifact("SourceArtifact");

        Pipeline pipeline = Pipeline.Builder.create(this,  "BFPServicePipeline")
                .pipelineName("BFPServicePipelineCDK")
                .pipelineType(PipelineType.V2)
                .executionMode(ExecutionMode.QUEUED)
                .artifactBucket(new Bucket(this, "BFPServicePipelineArtifactStore"))
                .role(Role.fromRoleArn(this, "BFPServicePipelineRole", "arn:aws:iam::891377256793:role/service-role/AWSCodePipelineServiceRole-us-west-2-BFPService"))
                .triggers(List.of(
                                TriggerProps.builder()
                                        .gitConfiguration(GitConfiguration.builder()
                                                .pushFilter(List.of(
                                                        GitPushFilter.builder()
                                                                .branchesIncludes(List.of("master"))
                                                                .build()
                                                ))
                                                .sourceAction(CodeStarConnectionsSourceAction.Builder.create()
                                                        .actionName("Source")
                                                        .connectionArn(CODESTAR_CONNECTION_ARN)
                                                        .owner("panosmoisiadis")
                                                        .repo("bfp")
                                                        .branch("master")
                                                        .output(sourceArtifact)
                                                        .triggerOnPush(true)
                                                        .variablesNamespace("SourceVariables")
                                                        .build())
                                                .build())
                                        .providerType(ProviderType.CODE_STAR_SOURCE_CONNECTION)
                                        .build()
                ))
                .stages(List.of(
                        StageProps.builder()
                                .stageName("Source")
                                .actions(List.of(
                                        CodeStarConnectionsSourceAction.Builder.create()
                                                .actionName("Source")
                                                .connectionArn(CODESTAR_CONNECTION_ARN)
                                                .owner("panosmoisiadis")
                                                .repo("bfp")
                                                .branch("master")
                                                .output(sourceArtifact)
                                                .triggerOnPush(true)
                                                .variablesNamespace("SourceVariables")
                                                .build()
                                ))
                                .build(),
                        StageProps.builder()
                                .stageName("Build")
                                .actions(List.of(
                                        CodeBuildAction.Builder.create()
                                                .actionName("Build")
                                                .type(CodeBuildActionType.BUILD)
                                                .input(sourceArtifact)
                                                .project(
                                                        PipelineProject.Builder.create(this, "BFPPipelineBuild")
                                                                .role(Role.fromRoleArn(this, "BFPBuildRole", "arn:aws:iam::891377256793:role/service-role/codebuild-BFPBuild-service-role"))
                                                                .buildSpec(BuildSpec.fromSourceFilename("buildfiles/deploy cdk.yaml"))
                                                                .environment(BuildEnvironment.builder()
                                                                        .computeType(ComputeType.LAMBDA_1GB)
                                                                        .buildImage(LinuxArmLambdaBuildImage.AMAZON_LINUX_2023_CORRETTO_21)
                                                                        .environmentVariables(Map.of(
                                                                                "STACK", BuildEnvironmentVariable.builder()
                                                                                        .value("BFPServicePipeline")
                                                                                        .type(BuildEnvironmentVariableType.PLAINTEXT)
                                                                                        .build()
                                                                        ))
                                                                        .build())
                                                                .build()
                                                )
                                                .build()
                                ))
                                .build()
                ))
                .build();
        return pipeline;
    }
}
