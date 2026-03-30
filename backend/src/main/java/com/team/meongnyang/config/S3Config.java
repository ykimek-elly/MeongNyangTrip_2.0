package com.team.meongnyang.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Value("${cloud.aws.region.static:ap-northeast-2}")
    private String region;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${cloud.aws.credentials.access-key:}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key:}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder().region(Region.of(region));

        if ("prod".equals(activeProfile)) {
            // EC2 운영: IAM Role 자동 사용
            builder.credentialsProvider(InstanceProfileCredentialsProvider.create());
        } else if (!accessKey.isBlank() && !secretKey.isBlank()) {
            // 로컬 개발: .env에 키가 있으면 사용
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)));
        } else {
            // 로컬 개발: ~/.aws/credentials 또는 환경변수 자동 탐색
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}