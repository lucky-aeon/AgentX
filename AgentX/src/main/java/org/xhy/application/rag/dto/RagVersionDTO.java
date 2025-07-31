package org.xhy.application.rag.dto;

import java.time.LocalDateTime;
import java.util.List;

/** RAG版本DTO
 * @author xhy
 * @date 2025-07-16 <br/>
 */
public class RagVersionDTO {

    /** 版本ID */
    private String id;

    /** 快照时的名称 */
    private String name;

    /** 快照时的图标 */
    private String icon;

    /** 快照时的描述 */
    private String description;

    /** 创建者ID */
    private String userId;

    /** 创建者昵称 */
    private String userNickname;

    /** 版本号 */
    private String version;

    /** 更新日志 */
    private String changeLog;

    /** 标签列表 */
    private List<String> labels;

    /** 原始RAG数据集ID */
    private String originalRagId;

    /** 原始RAG名称 */
    private String originalRagName;

    /** 文件数量 */
    private Integer fileCount;

    /** 总大小（字节） */
    private Long totalSize;

    /** 文档单元数量 */
    private Integer documentCount;

    /** 发布状态：1:审核中, 2:已发布, 3:拒绝, 4:已下架 */
    private Integer publishStatus;

    /** 发布状态描述 */
    private String publishStatusDesc;

    /** 审核拒绝原因 */
    private String rejectReason;

    /** 审核时间 */
    private LocalDateTime reviewTime;

    /** 发布时间 */
    private LocalDateTime publishedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 安装次数 */
    private Long installCount;

    /** 是否已安装（当前用户） */
    private Boolean isInstalled;

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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(String changeLog) {
        this.changeLog = changeLog;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public String getOriginalRagId() {
        return originalRagId;
    }

    public void setOriginalRagId(String originalRagId) {
        this.originalRagId = originalRagId;
    }

    public String getOriginalRagName() {
        return originalRagName;
    }

    public void setOriginalRagName(String originalRagName) {
        this.originalRagName = originalRagName;
    }

    public Integer getFileCount() {
        return fileCount;
    }

    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public Integer getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(Integer documentCount) {
        this.documentCount = documentCount;
    }

    public Integer getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(Integer publishStatus) {
        this.publishStatus = publishStatus;
    }

    public String getPublishStatusDesc() {
        return publishStatusDesc;
    }

    public void setPublishStatusDesc(String publishStatusDesc) {
        this.publishStatusDesc = publishStatusDesc;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public LocalDateTime getReviewTime() {
        return reviewTime;
    }

    public void setReviewTime(LocalDateTime reviewTime) {
        this.reviewTime = reviewTime;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getInstallCount() {
        return installCount;
    }

    public void setInstallCount(Long installCount) {
        this.installCount = installCount;
    }

    public Boolean getIsInstalled() {
        return isInstalled;
    }

    public void setIsInstalled(Boolean isInstalled) {
        this.isInstalled = isInstalled;
    }
}