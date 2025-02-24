package com.bfp.cdk;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.cognito.AccountRecovery;
import software.amazon.awscdk.services.cognito.AuthFlow;
import software.amazon.awscdk.services.cognito.CfnUserPoolUser;
import software.amazon.awscdk.services.cognito.CognitoDomainOptions;
import software.amazon.awscdk.services.cognito.Mfa;
import software.amazon.awscdk.services.cognito.PasswordPolicy;
import software.amazon.awscdk.services.cognito.StandardAttribute;
import software.amazon.awscdk.services.cognito.StandardAttributes;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolClient;
import software.amazon.awscdk.services.cognito.UserPoolDomain;
import software.amazon.awscdk.services.cognito.UserPoolEmail;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.AuthorizationConfig;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.ContainerDefinition;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.CpuArchitecture;
import software.amazon.awscdk.services.ecs.EfsVolumeConfiguration;
import software.amazon.awscdk.services.ecs.HealthCheck;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.MountPoint;
import software.amazon.awscdk.services.ecs.OperatingSystemFamily;
import software.amazon.awscdk.services.ecs.RuntimePlatform;
import software.amazon.awscdk.services.ecs.ScalableTaskCount;
import software.amazon.awscdk.services.ecs.Volume;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.efs.AccessPoint;
import software.amazon.awscdk.services.efs.AccessPointOptions;
import software.amazon.awscdk.services.efs.Acl;
import software.amazon.awscdk.services.efs.FileSystem;
import software.amazon.awscdk.services.efs.PosixUser;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.PostgresEngineVersion;
import software.amazon.awscdk.services.rds.PostgresInstanceEngineProps;
import software.amazon.awscdk.services.rds.SubnetGroup;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class BFPServiceStack extends StagedStack {
    public BFPServiceStack(final Construct parent, final String id, final BFPServiceStackProps props, Stage stage) {
        super(parent, id, props, stage);

        UserPool userPool = createUserPool();
        UserPoolClient userPoolClient = createUserPoolClient(userPool);
        UserPoolDomain userPoolDomain = createUserPoolDomain(userPool);
        Secret userPoolClientSecret = createUserPoolClientSecret(userPoolClient);

        if (getStage().equals(Stage.Dev)) {
            return;
        }

        VpcStack vpc = createVpc();
        DatabaseInstance database = createDatabase(vpc);
        Bucket fileBucket = createFileBucket();
        Repository repository = createRepository();
        if (props.shouldCreateEcsService()) {
            createEcs(userPool, userPoolClient, userPoolClientSecret, fileBucket, database, vpc, repository);
        }
    }

    void createEcs(UserPool userPool, UserPoolClient userPoolClient, Secret userPoolClientSecret, Bucket fileBucket, DatabaseInstance postgresInstance, VpcStack vpc,
                   Repository repository) {
        LogGroup logGroup = LogGroup.Builder.create(this, "BFPLogGroup")
                .logGroupName("BFP-" + getStage() + "-ApplicationLogs")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        SecurityGroup efsSecurityGroup = SecurityGroup.Builder.create(this, "EfsSecurityGroup")
                .vpc(vpc.getVpc())
                .description("Security group for EFS mount targets")
                .allowAllOutbound(true)
                .build();

        FileSystem fileSystem = FileSystem.Builder.create(this, "MyEfsFileSystem")
                .vpc(vpc.getVpc())
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(vpc.getVpc().getPrivateSubnets())
                        .build())
                .securityGroup(efsSecurityGroup)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        efsSecurityGroup.addIngressRule(
                vpc.getSecurityGroup(),
                Port.tcp(2049),
                "Allow NFS traffic from Fargate tasks"
        );

        AccessPoint accessPoint = fileSystem.addAccessPoint("MyEfsAccessPoint",
                AccessPointOptions.builder()
                        .path("/efs")
                        .createAcl(Acl.builder()
                                .ownerGid("1000")
                                .ownerUid("1000")
                                .permissions("755")
                                .build())
                        .posixUser(PosixUser.builder()
                                .gid("1000")
                                .uid("1000")
                                .build())
                        .build());

        PolicyDocument policyDocument = PolicyDocument.Builder.create()
                .statements(List.of(
                        PolicyStatement.Builder.create()
                                .effect(Effect.ALLOW)
                                .actions(List.of(
                                        "cognito-idp:AdminInitiateAuth",
                                        "cognito-idp:AdminUserGlobalSignOut"
                                ))
                                .resources(List.of(
                                        userPool.getUserPoolArn()
                                ))
                                .build(),
                        PolicyStatement.Builder.create()
                                .effect(Effect.ALLOW)
                                .actions(List.of(
                                        "s3:PutObject",
                                        "s3:GetObject",
                                        "s3:DeleteObject"
                                ))
                                .resources(List.of(
                                        fileBucket.getBucketArn(),
                                        fileBucket.getBucketArn() + "/*"
                                ))
                                .build(),
                        PolicyStatement.Builder.create()
                                .effect(Effect.ALLOW)
                                .actions(List.of(
                                        "secretsmanager:GetSecretValue"
                                ))
                                .resources(List.of(
                                        userPoolClientSecret.getSecretArn(),
                                        postgresInstance.getSecret().getSecretArn()
                                ))
                                .build())
                )
                .build();

        Role instanceRole = Role.Builder.create(this, "BFPInstanceRole")
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .inlinePolicies(Map.of("BFPInstanceRolePolicy", policyDocument))
                .build();

        ApplicationLoadBalancedFargateService fargateService = ApplicationLoadBalancedFargateService.Builder.create(this, "MyWebServer")
                .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                        .image(ContainerImage.fromEcrRepository(repository, "latest"))
                        .containerPort(8080)
                        .environment(Map.of(
                                "userpoolid", userPool.getUserPoolId(),
                                "userpoolclientid", userPoolClient.getUserPoolClientId(),
                                "fileBucketName", fileBucket.getBucketName(),
                                "efsMountPoint", "/efs"
                        ))
                        .secrets(Map.of(
                                "CLIENT_SECRET", software.amazon.awscdk.services.ecs.Secret.fromSecretsManager(userPoolClientSecret),
                                "postgreshost", software.amazon.awscdk.services.ecs.Secret.fromSecretsManager(postgresInstance.getSecret(), "host"),
                                "postgresusername", software.amazon.awscdk.services.ecs.Secret.fromSecretsManager(postgresInstance.getSecret(), "username"),
                                "postgrespassword", software.amazon.awscdk.services.ecs.Secret.fromSecretsManager(postgresInstance.getSecret(), "password")
                        ))
                        .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(logGroup)
                                .streamPrefix("BFPService")
                                .build())
                        )
                        .taskRole(instanceRole)
                        .build())
                .cpu(1024)
                .memoryLimitMiB(2048)
                .runtimePlatform(RuntimePlatform.builder()
                        .cpuArchitecture(CpuArchitecture.ARM64)
                        .operatingSystemFamily(OperatingSystemFamily.LINUX)
                        .build())
                .healthCheck(HealthCheck.builder()
                        .command(List.of("CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"))
                        .retries(3)
                        .timeout(Duration.seconds(5))
                        .interval(Duration.seconds(10))
                        .startPeriod(Duration.seconds(35))
                        .build())
                .publicLoadBalancer(true)
                .certificate(Certificate.fromCertificateArn(this, "ALBCertificate", "arn:aws:acm:us-east-1:891377256793:certificate/36687f83-04bb-42f8-9c7d-a4c115c671be"))
                .vpc(vpc.getVpc())
                .securityGroups(List.of(vpc.getSecurityGroup()))
                .taskSubnets(SubnetSelection.builder()
                        .subnets(vpc.getVpc().getPrivateSubnets())
                        .build())
                .build();

        Volume efsVolume = Volume.builder()
                .name("efs-volume")
                .efsVolumeConfiguration(EfsVolumeConfiguration.builder()
                        .fileSystemId(fileSystem.getFileSystemId())
                       .transitEncryption("ENABLED")
                        .authorizationConfig(AuthorizationConfig.builder()
                                .accessPointId(accessPoint.getAccessPointId())
                                .iam("ENABLED")
                                .build())
                        .build())
                .build();

        ContainerDefinition container = fargateService.getTaskDefinition().getDefaultContainer();
        container.addMountPoints(MountPoint.builder()
                .containerPath("/efs")
                .sourceVolume("efs-volume")
                .readOnly(false)
                .build());

        fargateService.getTaskDefinition().addVolume(efsVolume);

        fargateService.getTaskDefinition().getNode().addDependency(accessPoint);

        ScalableTaskCount count = fargateService.getService().autoScaleTaskCount(EnableScalingProps.builder()
                        .minCapacity(1)
                        .maxCapacity(20)
                .build());

        count.scaleOnCpuUtilization("CpuScaling", software.amazon.awscdk.services.ecs.CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(50)
                .build());

        fargateService.getTargetGroup().configureHealthCheck(software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck.builder()
                .path("/actuator/health")
                .healthyHttpCodes("200")
                .interval(Duration.seconds(10))
                .timeout(Duration.seconds(5))
                .healthyThresholdCount(2)
                .unhealthyThresholdCount(3)
                .build());
    }

    Repository createRepository() {
        Repository repository = Repository.Builder.create(this, "BFPRepository")
                .repositoryName("bfpservicerepository/" + getStage())
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
        return repository;
    }

    VpcStack createVpc() {
        VpcStack vpc = new VpcStack(this);
        return vpc;
    }

    Bucket createFileBucket() {
        Bucket fileBucket = new Bucket(this, "bfpfilebucket");
        fileBucket.applyRemovalPolicy(RemovalPolicy.DESTROY);

        new CfnOutput(this, "BFPFileBucket-" + getStage(), CfnOutputProps.builder()
                .value(fileBucket.getBucketName())
                .description("File bucket name")
                .build());

        return fileBucket;
    }

    DatabaseInstance createDatabase(VpcStack vpc) {
        SubnetGroup subnetGroup = SubnetGroup.Builder.create(this, "BFPSubnetGroup")
                .vpc(vpc.getVpc())
                .description("BFPSubnetGroup")
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_ISOLATED)
                        .build())
                .build();

        SecurityGroup rdsSecurityGroup = SecurityGroup.Builder.create(this, "BFPDatabaseSecurityGroup")
                .vpc(vpc.getVpc())
                .build();
        rdsSecurityGroup.addIngressRule(vpc.getSecurityGroup(), Port.POSTGRES, "Allow postgres traffic.");
        rdsSecurityGroup.addEgressRule(vpc.getSecurityGroup(), Port.POSTGRES, "Allow postgres traffic.");

        DatabaseInstance postgresInstance = DatabaseInstance.Builder.create(this, "BFPDatabaseInstance")
                .engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder()
                        .version(PostgresEngineVersion.VER_17_2)
                        .build()))
                .instanceType(InstanceType.of(InstanceClass.T4G, InstanceSize.MICRO))
                .vpc(vpc.getVpc())
                .securityGroups(List.of(rdsSecurityGroup))
                .subnetGroup(subnetGroup)
                .enablePerformanceInsights(false)
                .databaseName("postgres")
                .backupRetention(Duration.days(0))
                .autoMinorVersionUpgrade(true)
                .multiAz(false)
                .build();
        return postgresInstance;
    }

    UserPool createUserPool() {
        UserPool userPool = UserPool.Builder.create(this, "BFPUserPool")
                .passwordPolicy(PasswordPolicy.builder()
                        .minLength(8)
                        .requireLowercase(false)
                        .requireUppercase(false)
                        .requireDigits(false)
                        .requireSymbols(false)
                        .build())
                .accountRecovery(AccountRecovery.NONE)
                .deletionProtection(false)
                .email(UserPoolEmail.withCognito())
                .signInCaseSensitive(true)
                .selfSignUpEnabled(false)
                .mfa(Mfa.OFF)
                .standardAttributes(StandardAttributes.builder()
                        .email(StandardAttribute.builder()
                                .required(true)
                                .build())
                        .build())
                .build();
        userPool.applyRemovalPolicy(RemovalPolicy.DESTROY);

        new CfnOutput(this, "BFPUserPool-" + getStage() + " output", CfnOutputProps.builder()
                .value(userPool.getUserPoolId())
                .description("UserPoolId")
                .build());

        CfnUserPoolUser userPoolUser = CfnUserPoolUser.Builder.create(this, "BFPUserPoolUser")
                .userPoolId(userPool.getUserPoolId())
                .username("panosmoisiadis")
                .build();

        return userPool;
    }

    UserPoolClient createUserPoolClient(UserPool userPool) {
        UserPoolClient userPoolClient = UserPoolClient.Builder.create(this, "BFPUserPoolClient")
                .userPool(userPool)
                .authFlows(AuthFlow.builder()
                        .userPassword(true)
                        .adminUserPassword(true)
                        .build())
                .generateSecret(true)
                .build();

        new CfnOutput(this, "BFPUserPoolClient-" + getStage() + " output", CfnOutputProps.builder()
                .value(userPoolClient.getUserPoolClientId())
                .description("UserPoolClientId")
                .build());

        return userPoolClient;
    }

    Secret createUserPoolClientSecret(UserPoolClient userPoolClient) {
        Secret secret = Secret.Builder.create(this, "BFPSecret")
                .secretName("BFPUserPoolClientSecret-" + getStage())
                .secretName(userPoolClient.getUserPoolClientId())
                .secretStringValue(userPoolClient.getUserPoolClientSecret())
                .build();
        return secret;
    }

    UserPoolDomain createUserPoolDomain(UserPool userPool) {
        UserPoolDomain domain = UserPoolDomain.Builder.create(this, "BFPUserPoolDomain")
                .userPool(userPool)
                .cognitoDomain(CognitoDomainOptions.builder()
                        .domainPrefix("bfp-" + getStage())
                        .build())
                .build();

        new CfnOutput(this, "BFPUserPoolDomain-" + getStage() + " output", CfnOutputProps.builder()
                .value(domain.getDomainName())
                .description("UserPoolDomain")
                .build());

        return domain;
    }
}
