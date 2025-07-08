package org.xhy.infrastructure.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xhy.infrastructure.config.S3Properties;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/** S3对象存储服务实现 支持阿里云OSS通过S3协议访问 */
@Service
@Primary
@Conditional(S3EnabledCondition.class)
public class S3StorageService implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public S3StorageService(S3Properties s3Properties) {
        this.s3Properties = s3Properties;
        // 验证必要配置
        validateConfiguration();
        this.s3Client = createS3Client();
        logger.info("S3存储服务初始化成功，已连接到端点：{}", s3Properties.getEndpoint());
    }

    /** 验证配置是否完整 */
    private void validateConfiguration() {
        if (!StringUtils.hasText(s3Properties.getAccessKey())) {
            throw new IllegalStateException("S3配置错误：access-key 不能为空");
        }
        if (!StringUtils.hasText(s3Properties.getSecretKey())) {
            throw new IllegalStateException("S3配置错误：secret-key 不能为空");
        }
        if (!StringUtils.hasText(s3Properties.getEndpoint())) {
            throw new IllegalStateException("S3配置错误：endpoint 不能为空");
        }
        if (!StringUtils.hasText(s3Properties.getBucketName())) {
            throw new IllegalStateException("S3配置错误：bucket-name 不能为空");
        }
    }

    /** 创建S3客户端 */
    private S3Client createS3Client() {
        try {
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(s3Properties.getAccessKey(),
                    s3Properties.getSecretKey());

            S3Configuration s3Config = S3Configuration.builder()
                    .pathStyleAccessEnabled(s3Properties.isPathStyleAccess()).build();

            return S3Client.builder().endpointOverride(URI.create(s3Properties.getEndpoint()))
                    .region(Region.of(s3Properties.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .serviceConfiguration(s3Config).httpClient(UrlConnectionHttpClient.builder().build()).build();
        } catch (Exception e) {
            logger.error("创建S3客户端失败", e);
            throw new RuntimeException("创建S3客户端失败", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public UploadResult uploadFile(File file, String objectKey) {
        return uploadFile(file, objectKey, s3Properties.getBucketName());
    }

    @Override
    public UploadResult uploadFile(File file, String objectKey, String bucketName) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            // 计算文件MD5 (十六进制，用于记录)
            String md5Hash = calculateMD5Hex(file);

            // 构建上传请求 (不设置contentMD5，让SDK自动计算)
            PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(bucketName).key(objectKey)
                    .contentLength(file.length()).contentType(getContentType(file.getName())).build();

            // 执行上传
            PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(fileInputStream, file.length()));

            // 构建访问URL
            String accessUrl = buildAccessUrl(bucketName, objectKey);

            logger.info("文件上传成功: bucket={}, key={}, size={}, etag={}", bucketName, objectKey, file.length(),
                    putObjectResponse.eTag());

            return new UploadResult(UUID.randomUUID().toString(), file.getName(), objectKey, file.length(),
                    getContentType(file.getName()), bucketName, objectKey, accessUrl, md5Hash,
                    putObjectResponse.eTag());

        } catch (Exception e) {
            logger.error("文件上传失败: bucket={}, key={}", bucketName, objectKey, e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public UploadResult uploadStream(InputStream inputStream, String objectKey, long contentLength) {
        return uploadStream(inputStream, objectKey, contentLength, s3Properties.getBucketName());
    }

    @Override
    public UploadResult uploadStream(InputStream inputStream, String objectKey, long contentLength, String bucketName) {
        try {
            // 构建上传请求
            PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(bucketName).key(objectKey)
                    .contentLength(contentLength).build();

            // 执行上传
            PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(inputStream, contentLength));

            // 构建访问URL
            String accessUrl = buildAccessUrl(bucketName, objectKey);

            logger.info("输入流上传成功: bucket={}, key={}, size={}, etag={}", bucketName, objectKey, contentLength,
                    putObjectResponse.eTag());

            return new UploadResult(UUID.randomUUID().toString(), extractFileName(objectKey), objectKey, contentLength,
                    getContentType(objectKey), bucketName, objectKey, accessUrl, null, putObjectResponse.eTag());

        } catch (Exception e) {
            logger.error("输入流上传失败: bucket={}, key={}", bucketName, objectKey, e);
            throw new RuntimeException("输入流上传失败", e);
        }
    }

    @Override
    public boolean deleteFile(String objectKey) {
        return deleteFile(objectKey, s3Properties.getBucketName());
    }

    @Override
    public boolean deleteFile(String objectKey, String bucketName) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucketName).key(objectKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

            logger.info("文件删除成功: bucket={}, key={}", bucketName, objectKey);
            return true;

        } catch (Exception e) {
            logger.error("文件删除失败: bucket={}, key={}", bucketName, objectKey, e);
            return false;
        }
    }

    @Override
    public boolean fileExists(String objectKey) {
        return fileExists(objectKey, s3Properties.getBucketName());
    }

    @Override
    public boolean fileExists(String objectKey, String bucketName) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket(bucketName).key(objectKey).build();

            HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
            return headObjectResponse != null;

        } catch (Exception e) {
            logger.debug("文件不存在: bucket={}, key={}", bucketName, objectKey);
            return false;
        }
    }

    @Override
    public String generateObjectKey(String originalFileName, String folder) {
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID().toString() + getFileExtension(originalFileName);

        if (folder != null && !folder.isEmpty()) {
            return folder + "/" + datePath + "/" + fileName;
        } else {
            return datePath + "/" + fileName;
        }
    }

    /** 构建访问URL */
    private String buildAccessUrl(String bucketName, String objectKey) {
        if (s3Properties.getCustomDomain() != null && !s3Properties.getCustomDomain().isEmpty()) {
            return s3Properties.getCustomDomain() + "/" + objectKey;
        } else if (s3Properties.getUrlPrefix() != null && !s3Properties.getUrlPrefix().isEmpty()) {
            return s3Properties.getUrlPrefix() + "/" + objectKey;
        } else {
            // 对于阿里云OSS，使用虚拟主机样式访问
            if (s3Properties.isPathStyleAccess()) {
                // 路径样式：https://oss-cn-beijing.aliyuncs.com/bucket-name/object-key
                return s3Properties.getEndpoint() + "/" + bucketName + "/" + objectKey;
            } else {
                // 虚拟主机样式：https://bucket-name.oss-cn-beijing.aliyuncs.com/object-key
                String endpoint = s3Properties.getEndpoint().replace("https://", "");
                return "https://" + bucketName + "." + endpoint + "/" + objectKey;
            }
        }
    }

    /** 计算文件MD5（十六进制） */
    private String calculateMD5Hex(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) != -1) {
                md.update(buffer, 0, length);
            }
        }

        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /** 获取文件扩展名 */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }

    /** 从对象键中提取文件名 */
    private String extractFileName(String objectKey) {
        if (objectKey == null) {
            return "unknown";
        }
        int lastSlashIndex = objectKey.lastIndexOf('/');
        return lastSlashIndex >= 0 ? objectKey.substring(lastSlashIndex + 1) : objectKey;
    }

    @Override
    public byte[] downloadFile(String objectKey) {
        return downloadFile(objectKey, s3Properties.getBucketName());
    }

    @Override
    public byte[] downloadFile(String objectKey, String bucketName) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            byte[] bytes = s3Client.getObject(getObjectRequest).readAllBytes();

            logger.info("文件下载成功: bucket={}, key={}, size={}", bucketName, objectKey, bytes.length);
            return bytes;

        } catch (Exception e) {
            logger.error("文件下载失败: bucket={}, key={}", bucketName, objectKey, e);
            throw new RuntimeException("文件下载失败", e);
        }
    }

    /** 根据文件名获取内容类型 */
    private String getContentType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }

        String extension = getFileExtension(fileName).toLowerCase();
        switch (extension) {
            case ".jpg" :
            case ".jpeg" :
                return "image/jpeg";
            case ".png" :
                return "image/png";
            case ".gif" :
                return "image/gif";
            case ".pdf" :
                return "application/pdf";
            case ".txt" :
                return "text/plain";
            case ".json" :
                return "application/json";
            case ".xml" :
                return "application/xml";
            default :
                return "application/octet-stream";
        }
    }
}