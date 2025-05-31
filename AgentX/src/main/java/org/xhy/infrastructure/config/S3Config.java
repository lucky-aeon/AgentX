package org.xhy.infrastructure.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * S3客户端配置
 */
@Configuration
public class S3Config {

    private final S3Properties s3Properties;

    public S3Config(S3Properties s3Properties) {
        this.s3Properties = s3Properties;
    }

    /**
     * 创建S3客户端Bean
     * 
     * @return S3客户端实例
     */
    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                s3Properties.getAccessKey(),
                s3Properties.getSecretKey());

        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(s3Properties.isPathStyleAccess())
                .build();

        return S3Client.builder()
                .endpointOverride(URI.create(s3Properties.getEndpoint()))
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .serviceConfiguration(s3Config)
                .httpClient(UrlConnectionHttpClient.builder().build())
                .build();
    }
} 