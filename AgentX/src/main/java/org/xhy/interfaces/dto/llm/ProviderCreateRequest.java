package org.xhy.interfaces.dto.llm;

import org.xhy.domain.llm.model.config.ProviderConfig;

/**
 * 服务提供商创建请求
 */
public class ProviderCreateRequest {

    /**
     * 服务商代码
     */ 
    private String protocol;

    /**
     * 服务商名称
     */
    private String name;

    /**
     * 服务商描述
     */
    private String description;

    /**
     * 服务商配置
     */
    private ProviderConfig config;

    /**
     * 服务商状态
     */
    private Boolean status = true;

    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
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