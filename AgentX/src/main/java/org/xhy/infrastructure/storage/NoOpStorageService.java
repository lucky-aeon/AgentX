package org.xhy.infrastructure.storage;

import java.io.File;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** S3存储服务禁用条件 */
class S3DisabledCondition extends SpringBootCondition {
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String accessKey = context.getEnvironment().getProperty("s3.access-key");
        String secretKey = context.getEnvironment().getProperty("s3.secret-key");
        String endpoint = context.getEnvironment().getProperty("s3.endpoint");
        String bucketName = context.getEnvironment().getProperty("s3.bucket-name");

        boolean hasValidConfig = StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey)
                && StringUtils.hasText(endpoint) && StringUtils.hasText(bucketName);

        if (!hasValidConfig) {
            return ConditionOutcome.match("S3配置不完整，使用NoOp存储服务");
        } else {
            return ConditionOutcome.noMatch("S3配置完整，不使用NoOp存储服务");
        }
    }
}

/** 无操作存储服务实现 当S3配置不完整时使用，提供友好的错误提示 */
@Service
@Conditional(S3DisabledCondition.class)
public class NoOpStorageService implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(NoOpStorageService.class);

    private static final String ERROR_MESSAGE = "S3存储服务未配置或配置不完整，无法使用存储功能。请配置以下参数：s3.access-key, s3.secret-key, s3.endpoint, s3.bucket-name";

    public NoOpStorageService() {
        logger.warn("S3存储服务配置不完整，存储相关功能将不可用。请检查以下配置项：");
        logger.warn("  - s3.access-key");
        logger.warn("  - s3.secret-key");
        logger.warn("  - s3.endpoint");
        logger.warn("  - s3.bucket-name");
        logger.warn("如需使用存储功能，请在application.yml中正确配置S3相关参数");
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public UploadResult uploadFile(File file, String objectKey) {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public UploadResult uploadFile(File file, String objectKey, String bucketName) {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public UploadResult uploadStream(InputStream inputStream, String objectKey, long contentLength) {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public UploadResult uploadStream(InputStream inputStream, String objectKey, long contentLength, String bucketName) {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public boolean deleteFile(String objectKey) {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public boolean deleteFile(String objectKey, String bucketName) {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public boolean fileExists(String objectKey) {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public boolean fileExists(String objectKey, String bucketName) {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public String generateObjectKey(String originalFileName, String folder) {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public byte[] downloadFile(String objectKey) {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public byte[] downloadFile(String objectKey, String bucketName) {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }
}