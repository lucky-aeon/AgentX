package org.xhy.domain.user.model.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 降级配置 */
public class FallbackConfig implements Serializable {

    /** 是否启用降级 */
    private boolean enabled = false;

    /** 降级链，按优先级排序的模型ID列表 */
    private List<String> fallbackChain = new ArrayList<>();

    public FallbackConfig() {
    }

    public FallbackConfig(boolean enabled, List<String> fallbackChain) {
        this.enabled = enabled;
        this.fallbackChain = fallbackChain != null ? fallbackChain : new ArrayList<>();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getFallbackChain() {
        return fallbackChain;
    }

    public void setFallbackChain(List<String> fallbackChain) {
        this.fallbackChain = fallbackChain != null ? fallbackChain : new ArrayList<>();
    }

    /** 添加降级模型到链中 */
    public void addFallbackModel(String modelId) {
        if (modelId != null && !fallbackChain.contains(modelId)) {
            fallbackChain.add(modelId);
        }
    }

    /** 移除降级模型 */
    public void removeFallbackModel(String modelId) {
        fallbackChain.remove(modelId);
    }

}