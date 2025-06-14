package org.xhy.domain.apikey.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.xhy.infrastructure.entity.BaseEntity;

import java.time.LocalDateTime;

/** API密钥实体类 */
@TableName(value = "api_keys", autoResultMap = true)
public class ApiKeyEntity extends BaseEntity {

    /** API Key ID */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** API密钥 */
    @TableField("api_key")
    private String apiKey;

    /** 关联的Agent ID */
    @TableField("agent_id")
    private String agentId;

    /** 创建者用户ID */
    @TableField("user_id")
    private String userId;

    /** API Key名称/描述 */
    @TableField("name")
    private String name;

    /** 状态：TRUE-启用，FALSE-禁用 */
    @TableField("status")
    private Boolean status = true;

    /** 已使用次数 */
    @TableField("usage_count")
    private Integer usageCount = 0;

    /** 最后使用时间 */
    @TableField("last_used_at")
    private LocalDateTime lastUsedAt;

    /** 过期时间 */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    /** 生成API Key 格式：ak_{agentId}_{随机字符串} */
    public void generateApiKey() {
        String randomStr = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        this.apiKey = "ak_" + this.agentId + "_" + randomStr;
    }

    /** 检查API Key是否过期 */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /** 检查API Key是否可用 */
    public boolean isAvailable() {
        return status && !isExpired();
    }

    /** 增加使用次数 */
    public void incrementUsage() {
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
        this.lastUsedAt = LocalDateTime.now();
    }
}