package com.bfp.cdk;

import lombok.Getter;
import software.amazon.awscdk.services.ec2.AmazonLinux2023ImageSsmParameterProps;
import software.amazon.awscdk.services.ec2.AmazonLinuxCpuType;
import software.amazon.awscdk.services.ec2.Instance;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.MachineImage;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.constructs.Construct;

import java.util.List;

@Getter
public class VpcStack {
    private final Vpc vpc;
    private final SecurityGroup securityGroup;
    private final Instance bastionInstance;

    public VpcStack(Construct scope) {

        vpc = Vpc.Builder.create(scope, "SimpleVpc")
                .maxAzs(2)
                .natGateways(1)
                .subnetConfiguration(List.of(
                        SubnetConfiguration.builder()
                                .cidrMask(24)
                                .name("Public")
                                .subnetType(SubnetType.PUBLIC)
                                .build(),
                        SubnetConfiguration.builder()
                                .cidrMask(24)
                                .name("Private")
                                .subnetType(SubnetType.PRIVATE_ISOLATED)
                                .build(),
                        SubnetConfiguration.builder()
                                .cidrMask(24)
                                .name("Private with egress")
                                .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                                .build()
                ))
                .build();

        securityGroup = SecurityGroup.Builder.create(scope, "BFPSecurityGroup")
                .vpc(vpc)
                .allowAllOutbound(true)
                .build();

        Role role = Role.Builder.create(scope, "BFPBastionInstanceRole")
                .managedPolicies(List.of(
                        ManagedPolicy.fromManagedPolicyArn(scope, "SSMPolicy", "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore")
                ))
                .assumedBy(new ServicePrincipal("ec2.amazonaws.com"))
                .build();

        bastionInstance = Instance.Builder.create(scope, "BastionHost")
                .vpc(vpc)
                .securityGroup(securityGroup)
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                        .build())
                .machineImage(MachineImage.latestAmazonLinux2023(AmazonLinux2023ImageSsmParameterProps.builder()
                        .cpuType(AmazonLinuxCpuType.ARM_64)
                        .build()
                ))
                .role(role)
                .instanceType(InstanceType.of(InstanceClass.T4G, InstanceSize.NANO))
                .build();

        bastionInstance.addUserData("sudo yum install postgresql16 -y", "systemctl enable amazon-ssm-agent");
    }
}
