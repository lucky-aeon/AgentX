package org.xhy.infrastructure.storage;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * S3存储服务启用条件
 * 检查S3配置是否完整且有效
 */
public class S3EnabledCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String accessKey = context.getEnvironment().getProperty("s3.access-key");
        String secretKey = context.getEnvironment().getProperty("s3.secret-key");
        String endpoint = context.getEnvironment().getProperty("s3.endpoint");
        String bucketName = context.getEnvironment().getProperty("s3.bucket-name");

        boolean hasValidConfig = StringUtils.hasText(accessKey) 
            && StringUtils.hasText(secretKey)
            && StringUtils.hasText(endpoint)
            && StringUtils.hasText(bucketName);

        if (hasValidConfig) {
            return ConditionOutcome.match("S3配置完整，启用S3存储服务");
        } else {
            return ConditionOutcome.noMatch("S3配置不完整，使用NoOp存储服务");
        }
    }
} 