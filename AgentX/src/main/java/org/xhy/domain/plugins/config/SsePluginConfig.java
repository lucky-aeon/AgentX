package org.xhy.domain.plugins.config;

import org.xhy.domain.plugins.constant.PluginType;
import org.xhy.infrastructure.exception.BusinessException;

/**
 * SSE插件配置
 */
public class SsePluginConfig extends PluginConfig {
    private String url;
    
    public SsePluginConfig(String url) {
        this.url = url;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    @Override
    public boolean validate() {
        if (!getConfigType().equals(PluginType.SSE.getName())) {
            throw new BusinessException("配置类型不匹配, 预期类型: " + PluginType.SSE.getName());
        }
        if(!(url != null && !url.trim().isEmpty())) {
            throw new BusinessException("SSE插件配置无效");
        }
        return true;
    }
}