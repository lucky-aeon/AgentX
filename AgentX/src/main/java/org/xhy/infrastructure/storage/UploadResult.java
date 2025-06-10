package org.xhy.infrastructure.storage;

import java.time.LocalDateTime;

/** 上传结果类 */
public class UploadResult {
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

    public UploadResult(String fileId, String originalName, String storageName, Long fileSize, String contentType,
            String bucketName, String filePath, String accessUrl, String md5Hash, String etag) {
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
    public String getFileId() {
        return fileId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getStorageName() {
        return storageName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getAccessUrl() {
        return accessUrl;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    public String getEtag() {
        return etag;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "UploadResult{" + "fileId='" + fileId + '\'' + ", originalName='" + originalName + '\''
                + ", storageName='" + storageName + '\'' + ", fileSize=" + fileSize + ", contentType='" + contentType
                + '\'' + ", bucketName='" + bucketName + '\'' + ", filePath='" + filePath + '\'' + ", accessUrl='"
                + accessUrl + '\'' + ", md5Hash='" + md5Hash + '\'' + ", etag='" + etag + '\'' + ", createdAt="
                + createdAt + '}';
    }
}