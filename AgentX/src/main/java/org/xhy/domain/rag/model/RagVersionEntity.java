package org.xhy.domain.rag.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import org.xhy.infrastructure.converter.ListStringConverter;
import org.xhy.infrastructure.entity.BaseEntity;

/** RAG版本实体（完整快照）
 * @author xhy
 * @date 2025-07-16 <br/>
 */
@TableName("rag_versions")
public class RagVersionEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -1L;

    /** 版本ID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 快照时的名称 */
    private String name;

    /** 快照时的图标 */
    private String icon;

    /** 快照时的描述 */
    private String description;

    /** 创建者ID */
    private String userId;

    /** 版本号 (如 "1.0.0") */
    private String version;

    /** 更新日志 */
    private String changeLog;

    /** 标签列表 */
    @TableField(value = "labels", typeHandler = ListStringConverter.class)
    private List<String> labels;

    /** 原始RAG数据集ID（仅标识用） */
    private String originalRagId;

    /** 原始RAG名称（快照时） */
    private String originalRagName;

    /** 文件数量 */
    private Integer fileCount;

    /** 总大小（字节） */
    private Long totalSize;

    /** 文档单元数量 */
    private Integer documentCount;

    /** 发布状态：1:审核中, 2:已发布, 3:拒绝, 4:已下架 */
    private Integer publishStatus;

    /** 审核拒绝原因 */
    private String rejectReason;

    /** 审核时间 */
    private LocalDateTime reviewTime;

    /** 发布时间 */
    private LocalDateTime publishedAt;

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
}