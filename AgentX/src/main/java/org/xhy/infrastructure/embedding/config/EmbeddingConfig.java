package org.xhy.infrastructure.embedding.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

/**
 * 嵌入式配置
 * @author shilong.zang
 * @date 14:48 <br/>
 */
@Configuration
@EnableConfigurationProperties(EmbeddingProperties.class)
public class EmbeddingConfig {

    private final EmbeddingProperties embeddingProperties;

    /**
     * 构造方法，注入配置属性
     * @param embeddingProperties 嵌入服务配置属性
     */
    public EmbeddingConfig(EmbeddingProperties embeddingProperties) {
        this.embeddingProperties = embeddingProperties;
    }

    /**
     * 创建OpenAI嵌入模型Bean
     * @return OpenAiEmbeddingModel实例
     */
    @Bean
    OpenAiEmbeddingModel openAiEmbeddingModel() {
        // 从配置属性中提取基础URL，如果apiUrl包含完整路径则需要提取基础部分
        String baseUrl = embeddingProperties.getApiUrl();

        return OpenAiEmbeddingModel.builder()
                .apiKey(embeddingProperties.getApiKey())
                .baseUrl(baseUrl)
                .modelName(embeddingProperties.getModel())
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
