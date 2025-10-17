package org.xhy.domain.rag.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.xhy.domain.llm.model.enums.ModelType;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

import java.io.Serial;
import java.io.Serializable;

/** RAG模型配置
 * 
 * @author shilong.zang */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** API密钥 */
    private String apiKey;

    /** API基础URL */
    private String baseUrl;

    private ModelType modelType;

    private ProviderProtocol protocol;

    private String modelEndpoint;

    public ModelConfig() {
    }

    public ModelConfig(String apiKey, String baseUrl, ModelType modelType, ProviderProtocol protocol,
            String modelEndpoint) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.modelType = modelType;
        this.protocol = protocol;
        this.modelEndpoint = modelEndpoint;
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

    @JsonIgnore
    public boolean isChatType() {
        return this.modelType == ModelType.CHAT;
    }

    public String getModelEndpoint() {
        return modelEndpoint;
    }

    public void setModelEndpoint(String modelEndpoint) {
        this.modelEndpoint = modelEndpoint;
    }
}
