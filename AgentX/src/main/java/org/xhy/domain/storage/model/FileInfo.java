package org.xhy.domain.storage.model;

import java.time.LocalDateTime;

/**
 * 文件信息领域模型
 */
public class FileInfo {

    /**
     * 文件唯一标识
     */
    private String fileId;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 存储文件名
     */
    private String storageName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String contentType;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件访问URL
     */
    private String accessUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 文件MD5值
     */
    private String md5Hash;

    // 构造函数
    public FileInfo() {
    }

    public FileInfo(String fileId, String originalName, String storageName, 
                   Long fileSize, String contentType, String bucketName, 
                   String filePath, String accessUrl, String md5Hash) {
        this.fileId = fileId;
        this.originalName = originalName;
        this.storageName = storageName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.bucketName = bucketName;
        this.filePath = filePath;
        this.accessUrl = accessUrl;
        this.md5Hash = md5Hash;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getStorageName() {
        return storageName;
    }

    public void setStorageName(String storageName) {
        this.storageName = storageName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getAccessUrl() {
        return accessUrl;
    }

    public void setAccessUrl(String accessUrl) {
        this.accessUrl = accessUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    public void setMd5Hash(String md5Hash) {
        this.md5Hash = md5Hash;
    }
} 