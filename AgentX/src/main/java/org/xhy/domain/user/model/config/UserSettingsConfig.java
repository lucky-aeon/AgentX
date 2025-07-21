package org.xhy.domain.user.model.config;

import java.io.Serializable;

/** 用户设置配置 */
public class UserSettingsConfig implements Serializable {

    /** 默认聊天模型ID */
    private String defaultModel;

    /** 默认OCR模型ID */
    private String defaultOcrModel;

    /** 默认嵌入模型ID */
    private String defaultEmbeddingModel;

    /** 降级配置 */
    private FallbackConfig fallbackConfig;

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public FallbackConfig getFallbackConfig() {
        return fallbackConfig;
    }

    public void setFallbackConfig(FallbackConfig fallbackConfig) {
        this.fallbackConfig = fallbackConfig;
    }

    public String getDefaultOcrModel() {
        return defaultOcrModel;
    }

    public void setDefaultOcrModel(String defaultOcrModel) {
        this.defaultOcrModel = defaultOcrModel;
    }

    public String getDefaultEmbeddingModel() {
        return defaultEmbeddingModel;
    }

    public void setDefaultEmbeddingModel(String defaultEmbeddingModel) {
        this.defaultEmbeddingModel = defaultEmbeddingModel;
    }

}