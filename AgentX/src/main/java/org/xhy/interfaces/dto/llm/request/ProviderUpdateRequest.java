package org.xhy.interfaces.dto.llm.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.xhy.domain.llm.model.config.ProviderConfig;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

/**
 * 服务提供商更新请求
 */
public class ProviderUpdateRequest {
    
    /**
     * 服务商ID
     */
    private String id;

    /**
     * 服务商描述
     */
    private String description;

    /**
     * 服务商协议
     */
    @NotNull(message = "协议不可为空")
    private ProviderProtocol protocol;

    /**
     * 服务商名称
     */
    @NotBlank(message = "名称不可为空")
    private String name;

    /**
     * 服务商配置
     */
    private ProviderConfig config;

    /**
     * 服务商状态
     */
    private Boolean status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ProviderProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(ProviderProtocol protocol) {
        this.protocol = protocol;
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

    public ProviderConfig getConfig() {
        return config;
    }

    public void setConfig(ProviderConfig config) {
        this.config = config;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }
}