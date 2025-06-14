package org.xhy.interfaces.dto.apikey.response;

import java.time.LocalDateTime;

/** API密钥响应对象 */
public class ApiKeyResponse {

    /** API Key ID */
    private String id;

    /** API密钥 */
    private String apiKey;

    /** 关联的Agent ID */
    private String agentId;

    /** 关联的Agent名称 */
    private String agentName;

    /** API Key名称/描述 */
    private String name;

    /** 状态：TRUE-启用，FALSE-禁用 */
    private Boolean status;

    /** 已使用次数 */
    private Integer usageCount;

    /** 最后使用时间 */
    private LocalDateTime lastUsedAt;

    /** 过期时间 */
    private LocalDateTime expiresAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 是否已过期 */
    private Boolean expired;

    /** 是否可用 */
    private Boolean available;

    public ApiKeyResponse() {
    }

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

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getExpired() {
        return expired;
    }

    public void setExpired(Boolean expired) {
        this.expired = expired;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
}