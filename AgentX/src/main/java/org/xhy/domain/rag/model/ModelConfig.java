package org.xhy.domain.rag.model;

import org.xhy.domain.llm.model.enums.ModelType;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

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

    private ModelType modelType;

    private ProviderProtocol protocol;

    public ModelConfig() {
    }

    public ModelConfig(String modelId, String apiKey, String baseUrl, ModelType modelType,ProviderProtocol protocol) {
        this.modelId = modelId;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.modelType = modelType;
        this.protocol = protocol;
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

    public ModelType getModelType() {
        return modelType;
    }

    public void setModelType(ModelType modelType) {
        this.modelType = modelType;
    }

    public ProviderProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(ProviderProtocol protocol) {
        this.protocol = protocol;
    }

    public boolean isChatType(){
        return this.modelType == ModelType.CHAT;
    }
}