package org.xhy.infrastructure.rag.factory;

import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.stereotype.Component;

/** 嵌入模型工厂类 根据用户配置动态创建嵌入模型实例
 * 
 * @author shilong.zang
 * @date 2025-01-22 */
@Component
public class EmbeddingModelFactory {

    /** 嵌入模型配置类 */
    public static class EmbeddingConfig {
        private String apiKey;
        private String baseUrl;
        private String modelEndpoint;

        public EmbeddingConfig() {
        }

        public EmbeddingConfig(String apiKey, String baseUrl, String modelName) {
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
            this.modelEndpoint = modelName;
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

        public String getModelEndpoint() {
            return modelEndpoint;
        }

        public void setModelEndpoint(String modelEndpoint) {
            this.modelEndpoint = modelEndpoint;
        }
    }

    /** 根据配置创建OpenAI嵌入模型实例
     * 
     * @param config 嵌入模型配置
     * @return OpenAiEmbeddingModel实例 */
    public OpenAiEmbeddingModel createEmbeddingModel(EmbeddingConfig config) {
        return OpenAiEmbeddingModel.builder().apiKey(config.getApiKey()).baseUrl(config.getBaseUrl())
                .modelName(config.getModelEndpoint()).build();
    }
}