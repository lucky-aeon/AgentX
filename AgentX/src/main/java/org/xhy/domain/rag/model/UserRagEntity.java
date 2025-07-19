package org.xhy.domain.rag.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import org.xhy.domain.rag.constant.InstallType;
import org.xhy.infrastructure.converter.InstallTypeConverter;
import org.xhy.infrastructure.entity.BaseEntity;

/** 用户安装的RAG实体
 * @author xhy
 * @date 2025-07-16 <br/>
 */
@TableName("user_rags")
public class UserRagEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -1L;

    /** 安装记录ID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 用户ID */
    private String userId;

    /** 关联的RAG版本快照ID */
    private String ragVersionId;

    /** 安装时的名称 */
    private String name;

    /** 安装时的描述 */
    private String description;

    /** 安装时的图标 */
    private String icon;

    /** 版本号 */
    private String version;


    /** 安装时间 */
    private LocalDateTime installedAt;

    /** 原始RAG数据集ID */
    private String originalRagId;

    /** 安装类型 */
    @TableField(value = "install_type", typeHandler = InstallTypeConverter.class)
    private InstallType installType = InstallType.SNAPSHOT;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRagVersionId() {
        return ragVersionId;
    }

    public void setRagVersionId(String ragVersionId) {
        this.ragVersionId = ragVersionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }


    public LocalDateTime getInstalledAt() {
        return installedAt;
    }

    public void setInstalledAt(LocalDateTime installedAt) {
        this.installedAt = installedAt;
    }

    public String getOriginalRagId() {
        return originalRagId;
    }

    public void setOriginalRagId(String originalRagId) {
        this.originalRagId = originalRagId;
    }

    public InstallType getInstallType() {
        return installType;
    }

    public void setInstallType(InstallType installType) {
        this.installType = installType;
    }


    /** 检查是否为引用类型安装
     * 
     * @return 是否为引用类型 */
    public boolean isReferenceType() {
        return this.installType != null && this.installType.isReference();
    }

    /** 检查是否为快照类型安装
     * 
     * @return 是否为快照类型 */
    public boolean isSnapshotType() {
        return this.installType != null && this.installType.isSnapshot();
    }
}