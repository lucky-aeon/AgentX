package org.xhy.domain.rag.model;

import java.io.Serial;
import java.io.Serializable;

/** RAG模型配置
 * 
 * @author shilong.zang */
public class ModelConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 模型ID */
    private String modelId;

    /** API密钥 */
    private String apiKey;

    /** API基础URL */
    private String baseUrl;

    /** 模型类型（OCR/EMBEDDING） */
    private String modelType;

    public ModelConfig() {
    }

    public ModelConfig(String modelId, String apiKey, String baseUrl, String modelType) {
        this.modelId = modelId;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.modelType = modelType;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }
}