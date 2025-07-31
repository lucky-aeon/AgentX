package org.xhy.application.rag.dto;

import java.time.LocalDateTime;
import java.util.List;

/** RAG市场DTO（用于市场展示）
 * @author xhy
 * @date 2025-07-16 <br/>
 */
public class RagMarketDTO {

    /** 版本ID */
    private String id;

    /** RAG名称 */
    private String name;

    /** RAG图标 */
    private String icon;

    /** RAG描述 */
    private String description;

    /** 版本号 */
    private String version;

    /** 标签列表 */
    private List<String> labels;

    /** 创建者ID */
    private String userId;

    /** 创建者昵称 */
    private String userNickname;

    /** 创建者头像 */
    private String userAvatar;

    /** 文件数量 */
    private Integer fileCount;

    /** 文档单元数量 */
    private Integer documentCount;

    /** 总大小（字节） */
    private Long totalSize;

    /** 总大小（格式化显示） */
    private String totalSizeDisplay;

    /** 安装次数 */
    private Long installCount;

    /** 发布时间 */
    private LocalDateTime publishedAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 是否已安装（当前用户） */
    private Boolean isInstalled;

    /** 评分（预留） */
    private Double rating;

    /** 评价数量（预留） */
    private Integer reviewCount;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public void setUserNickname(String userNickname) {
        this.userNickname = userNickname;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public Integer getFileCount() {
        return fileCount;
    }

    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }

    public Integer getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(Integer documentCount) {
        this.documentCount = documentCount;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public String getTotalSizeDisplay() {
        return totalSizeDisplay;
    }

    public void setTotalSizeDisplay(String totalSizeDisplay) {
        this.totalSizeDisplay = totalSizeDisplay;
    }

    public Long getInstallCount() {
        return installCount;
    }

    public void setInstallCount(Long installCount) {
        this.installCount = installCount;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getIsInstalled() {
        return isInstalled;
    }

    public void setIsInstalled(Boolean isInstalled) {
        this.isInstalled = isInstalled;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }
}