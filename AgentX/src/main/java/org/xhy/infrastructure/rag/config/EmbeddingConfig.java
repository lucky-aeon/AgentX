package org.xhy.infrastructure.rag.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;

/** 嵌入式配置
 * @author shilong.zang
 * @date 14:48 <br/>
 */
@Configuration
@EnableConfigurationProperties(EmbeddingProperties.class)
public class EmbeddingConfig {

    private final EmbeddingProperties embeddingProperties;

    /** 构造方法，注入配置属性
     * @param embeddingProperties 嵌入服务配置属性 */
    public EmbeddingConfig(EmbeddingProperties embeddingProperties) {
        this.embeddingProperties = embeddingProperties;
    }

    /** 向量化存储配置
     * @return PgVectorEmbeddingStore实例 */
    @Bean
    public EmbeddingStore<TextSegment> initEmbeddingStore() {
        EmbeddingProperties.VectorStore vectorStoreConfig = embeddingProperties.getVectorStore();

        return PgVectorEmbeddingStore.builder().table(vectorStoreConfig.getTable())
                .dropTableFirst(vectorStoreConfig.isDropTableFirst()).createTable(vectorStoreConfig.isCreateTable())
                .host(vectorStoreConfig.getHost()).port(vectorStoreConfig.getPort()).user(vectorStoreConfig.getUser())
                .password(vectorStoreConfig.getPassword()).dimension(vectorStoreConfig.getDimension())
                .database(vectorStoreConfig.getDatabase()).build();
    }

}
