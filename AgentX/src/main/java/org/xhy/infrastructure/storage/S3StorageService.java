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
import org.springframework.stereotype.Service;
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
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * S3对象存储服务
 * 支持阿里云OSS通过S3协议访问
 */
@Service
public class S3StorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public S3StorageService(S3Properties s3Properties) {
        this.s3Properties = s3Properties;
        this.s3Client = createS3Client();
    }

    /**
     * 创建S3客户端
     */
    private S3Client createS3Client() {
        try {
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
        } catch (Exception e) {
            logger.error("创建S3客户端失败", e);
            throw new RuntimeException("创建S3客户端失败", e);
        }
    }

    /**
     * 上传文件
     * 
     * @param file 本地文件
     * @param objectKey 对象存储中的文件路径
     * @return 上传结果信息
     */
    public UploadResult uploadFile(File file, String objectKey) {
        return uploadFile(file, objectKey, s3Properties.getBucketName());
    }

    /**
     * 上传文件到指定桶
     * 
     * @param file 本地文件
     * @param objectKey 对象存储中的文件路径
     * @param bucketName 存储桶名称
     * @return 上传结果信息
     */
    public UploadResult uploadFile(File file, String objectKey, String bucketName) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            
            // 计算文件MD5 (十六进制，用于记录)
            String md5Hash = calculateMD5Hex(file);
            
            // 构建上传请求 (不设置contentMD5，让SDK自动计算)
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentLength(file.length())
                    .contentType(getContentType(file.getName()))
                    .build();

            // 执行上传
            PutObjectResponse putObjectResponse = s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(fileInputStream, file.length()));

            // 构建访问URL
            String accessUrl = buildAccessUrl(bucketName, objectKey);
            
            logger.info("文件上传成功: bucket={}, key={}, size={}, etag={}", 
                       bucketName, objectKey, file.length(), putObjectResponse.eTag());

            return new UploadResult(
                    UUID.randomUUID().toString(),
                    file.getName(),
                    objectKey,
                    file.length(),
                    getContentType(file.getName()),
                    bucketName,
                    objectKey,
                    accessUrl,
                    md5Hash,
                    putObjectResponse.eTag()
            );

        } catch (Exception e) {
            logger.error("文件上传失败: bucket={}, key={}", bucketName, objectKey, e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 上传输入流
     * 
     * @param inputStream 输入流
     * @param objectKey 对象存储中的文件路径
     * @param contentLength 内容长度
     * @return 上传结果信息
     */
    public UploadResult uploadStream(InputStream inputStream, String objectKey, long contentLength) {
        return uploadStream(inputStream, objectKey, contentLength, s3Properties.getBucketName());
    }

    /**
     * 上传输入流到指定桶
     * 
     * @param inputStream 输入流
     * @param objectKey 对象存储中的文件路径
     * @param contentLength 内容长度
     * @param bucketName 存储桶名称
     * @return 上传结果信息
     */
    public UploadResult uploadStream(InputStream inputStream, String objectKey, long contentLength, String bucketName) {
        try {
            // 构建上传请求
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentLength(contentLength)
                    .build();

            // 执行上传
            PutObjectResponse putObjectResponse = s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(inputStream, contentLength));

            // 构建访问URL
            String accessUrl = buildAccessUrl(bucketName, objectKey);
            
            logger.info("输入流上传成功: bucket={}, key={}, size={}, etag={}", 
                       bucketName, objectKey, contentLength, putObjectResponse.eTag());

            return new UploadResult(
                    UUID.randomUUID().toString(),
                    extractFileName(objectKey),
                    objectKey,
                    contentLength,
                    getContentType(objectKey),
                    bucketName,
                    objectKey,
                    accessUrl,
                    null,
                    putObjectResponse.eTag()
            );

        } catch (Exception e) {
            logger.error("输入流上传失败: bucket={}, key={}", bucketName, objectKey, e);
            throw new RuntimeException("输入流上传失败", e);
        }
    }

    /**
     * 删除文件
     * 
     * @param objectKey 对象存储中的文件路径
     * @return 是否删除成功
     */
    public boolean deleteFile(String objectKey) {
        return deleteFile(objectKey, s3Properties.getBucketName());
    }

    /**
     * 删除指定桶中的文件
     * 
     * @param objectKey 对象存储中的文件路径
     * @param bucketName 存储桶名称
     * @return 是否删除成功
     */
    public boolean deleteFile(String objectKey, String bucketName) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            
            logger.info("文件删除成功: bucket={}, key={}", bucketName, objectKey);
            return true;

        } catch (Exception e) {
            logger.error("文件删除失败: bucket={}, key={}", bucketName, objectKey, e);
            return false;
        }
    }

    /**
     * 检查文件是否存在
     * 
     * @param objectKey 对象存储中的文件路径
     * @return 是否存在
     */
    public boolean fileExists(String objectKey) {
        return fileExists(objectKey, s3Properties.getBucketName());
    }

    /**
     * 检查指定桶中的文件是否存在
     * 
     * @param objectKey 对象存储中的文件路径
     * @param bucketName 存储桶名称
     * @return 是否存在
     */
    public boolean fileExists(String objectKey, String bucketName) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
            return headObjectResponse != null;

        } catch (Exception e) {
            logger.debug("文件不存在: bucket={}, key={}", bucketName, objectKey);
            return false;
        }
    }

    /**
     * 生成唯一的对象键
     * 
     * @param originalFileName 原始文件名
     * @param folder 文件夹路径（可选）
     * @return 唯一的对象键
     */
    public String generateObjectKey(String originalFileName, String folder) {
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID().toString() + getFileExtension(originalFileName);
        
        if (folder != null && !folder.isEmpty()) {
            return folder + "/" + datePath + "/" + fileName;
        } else {
            return datePath + "/" + fileName;
        }
    }

    /**
     * 构建访问URL
     */
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

    /**
     * 计算文件MD5（十六进制）
     */
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

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }

    /**
     * 从对象键中提取文件名
     */
    private String extractFileName(String objectKey) {
        if (objectKey == null) {
            return "unknown";
        }
        int lastSlashIndex = objectKey.lastIndexOf('/');
        return lastSlashIndex >= 0 ? objectKey.substring(lastSlashIndex + 1) : objectKey;
    }

    /**
     * 根据文件名获取内容类型
     */
    private String getContentType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }
        
        String extension = getFileExtension(fileName).toLowerCase();
        switch (extension) {
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".pdf":
                return "application/pdf";
            case ".txt":
                return "text/plain";
            case ".json":
                return "application/json";
            case ".xml":
                return "application/xml";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * 上传结果类
     */
    public static class UploadResult {
        private final String fileId;
        private final String originalName;
        private final String storageName;
        private final Long fileSize;
        private final String contentType;
        private final String bucketName;
        private final String filePath;
        private final String accessUrl;
        private final String md5Hash;
        private final String etag;
        private final LocalDateTime createdAt;

        public UploadResult(String fileId, String originalName, String storageName, 
                           Long fileSize, String contentType, String bucketName, 
                           String filePath, String accessUrl, String md5Hash, String etag) {
            this.fileId = fileId;
            this.originalName = originalName;
            this.storageName = storageName;
            this.fileSize = fileSize;
            this.contentType = contentType;
            this.bucketName = bucketName;
            this.filePath = filePath;
            this.accessUrl = accessUrl;
            this.md5Hash = md5Hash;
            this.etag = etag;
            this.createdAt = LocalDateTime.now();
        }

        // Getters
        public String getFileId() { return fileId; }
        public String getOriginalName() { return originalName; }
        public String getStorageName() { return storageName; }
        public Long getFileSize() { return fileSize; }
        public String getContentType() { return contentType; }
        public String getBucketName() { return bucketName; }
        public String getFilePath() { return filePath; }
        public String getAccessUrl() { return accessUrl; }
        public String getMd5Hash() { return md5Hash; }
        public String getEtag() { return etag; }
        public LocalDateTime getCreatedAt() { return createdAt; }

        @Override
        public String toString() {
            return "UploadResult{" +
                    "fileId='" + fileId + '\'' +
                    ", originalName='" + originalName + '\'' +
                    ", storageName='" + storageName + '\'' +
                    ", fileSize=" + fileSize +
                    ", contentType='" + contentType + '\'' +
                    ", bucketName='" + bucketName + '\'' +
                    ", filePath='" + filePath + '\'' +
                    ", accessUrl='" + accessUrl + '\'' +
                    ", md5Hash='" + md5Hash + '\'' +
                    ", etag='" + etag + '\'' +
                    ", createdAt=" + createdAt +
                    '}';
        }
    }
} 