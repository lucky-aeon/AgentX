package org.xhy.infrastructure.memory.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Memory 向量存储 Bean 装配（独立表 public.memory_vector_store） */
@Configuration
@EnableConfigurationProperties(MemoryEmbeddingProperties.class)
public class MemoryEmbeddingConfig {

    private final MemoryEmbeddingProperties props;

    public MemoryEmbeddingConfig(MemoryEmbeddingProperties props) {
        this.props = props;
    }

    /** 记忆向量库（PgVector） */
    @Bean(name = "memoryEmbeddingStore")
    public EmbeddingStore<TextSegment> memoryEmbeddingStore() {
        MemoryEmbeddingProperties.VectorStore c = props.getVectorStore();
        return PgVectorEmbeddingStore.builder().table(c.getTable()).dropTableFirst(c.isDropTableFirst())
                .createTable(c.isCreateTable()).host(c.getHost()).port(c.getPort()).user(c.getUser())
                .password(c.getPassword()).dimension(c.getDimension()).database(c.getDatabase()).build();
    }
}
