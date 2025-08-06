package org.xhy.application.knowledgeGraph.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Neo4j模式管理服务
 * 负责自动创建和管理知识图谱的索引
 * 
 * @author zang
 */
@Service
@ConfigurationProperties(prefix = "agentx.graph.schema")
public class GraphSchemaManager {

    private static final Logger logger = LoggerFactory.getLogger(GraphSchemaManager.class);

    private final Neo4jClient neo4jClient;

    private boolean autoCreateIndexes = true;
    private List<IndexDefinition> initialIndexes;

    public GraphSchemaManager(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    /**
     * 初始化图谱模式 - 创建初始索引
     */
    public void initializeSchema() {
        if (!autoCreateIndexes) {
            logger.info("索引自动创建功能已禁用");
            return;
        }

        if (initialIndexes == null || initialIndexes.isEmpty()) {
            logger.info("未配置初始索引，跳过索引创建");
            return;
        }

        logger.info("开始创建知识图谱初始索引，共{}个索引", initialIndexes.size());

        int successCount = 0;
        int skipCount = 0;

        for (IndexDefinition indexDef : initialIndexes) {
            try {
                if (createIndexIfNotExists(indexDef)) {
                    successCount++;
                } else {
                    skipCount++;
                }
            } catch (Exception e) {
                logger.error("创建索引失败: {}.{} - {}", indexDef.getLabel(), indexDef.getProperty(), e.getMessage());
            }
        }

        logger.info("索引创建完成：成功{}个，跳过{}个", successCount, skipCount);
    }

    /**
     * 如果索引不存在则创建
     * 
     * @param indexDef 索引定义
     * @return true: 已创建, false: 已存在(跳过)
     */
    public boolean createIndexIfNotExists(IndexDefinition indexDef) {
        String label = indexDef.getLabel();
        String property = indexDef.getProperty();
        String indexName = generateIndexName(label, property);

        // 检查索引是否已存在
        if (indexExists(indexName)) {
            logger.debug("索引已存在，跳过: {}", indexName);
            return false;
        }

        // 创建索引
        String createIndexQuery = String.format(
            "CREATE INDEX %s IF NOT EXISTS FOR (n:`%s`) ON (n.`%s`)", 
            indexName, label, property
        );

        try {
            neo4jClient.query(createIndexQuery).run();
            logger.info("成功创建索引: {} - {}.{}", indexName, label, property);
            return true;
        } catch (Exception e) {
            logger.error("创建索引失败: {} - {}", indexName, e.getMessage());
            throw e;
        }
    }

    /**
     * 检查索引是否存在
     */
    private boolean indexExists(String indexName) {
        try {
            String checkQuery = "SHOW INDEXES YIELD name WHERE name = $indexName";
            List<Map<String, Object>> results = new ArrayList<>(neo4jClient.query(checkQuery)
                    .bind(indexName).to("indexName")
                    .fetch()
                    .all());

            return !results.isEmpty();
        } catch (Exception e) {
            logger.warn("检查索引存在性时发生错误，假设索引不存在: {} - {}", indexName, e.getMessage());
            return false;
        }
    }

    /**
     * 生成标准化索引名称
     */
    private String generateIndexName(String label, String property) {
        return String.format("idx_%s_%s", 
            sanitizeName(label), 
            sanitizeName(property));
    }

    /**
     * 清理名称用作索引名
     */
    private String sanitizeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_")
                  .toLowerCase();
    }

    /**
     * 获取所有现有索引信息
     */
    public List<Map<String, Object>> getAllIndexes() {
        try {
            String query = "SHOW INDEXES YIELD name, labelsOrTypes, properties, state";
            return new ArrayList<>(neo4jClient.query(query).fetch().all());
        } catch (Exception e) {
            logger.error("获取索引列表失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 删除指定索引
     */
    public void dropIndex(String indexName) {
        try {
            String dropQuery = String.format("DROP INDEX %s IF EXISTS", indexName);
            neo4jClient.query(dropQuery).run();
            logger.info("成功删除索引: {}", indexName);
        } catch (Exception e) {
            logger.error("删除索引失败: {} - {}", indexName, e.getMessage());
            throw e;
        }
    }

    /**
     * 获取索引统计信息
     */
    public Map<String, Object> getIndexStatistics() {
        try {
            String statsQuery = """
                SHOW INDEXES YIELD name, labelsOrTypes, properties, state, type
                RETURN count(*) AS totalIndexes,
                       collect(DISTINCT state) AS states,
                       collect(DISTINCT type) AS types
                """;

            return neo4jClient.query(statsQuery)
                    .fetch()
                    .one()
                    .orElse(Map.of("totalIndexes", 0, "states", List.of(), "types", List.of()));
        } catch (Exception e) {
            logger.error("获取索引统计信息失败: {}", e.getMessage());
            return Map.of("totalIndexes", 0, "error", e.getMessage());
        }
    }

    // Getters and Setters
    public boolean isAutoCreateIndexes() {
        return autoCreateIndexes;
    }

    public void setAutoCreateIndexes(boolean autoCreateIndexes) {
        this.autoCreateIndexes = autoCreateIndexes;
    }

    public List<IndexDefinition> getInitialIndexes() {
        return initialIndexes;
    }

    public void setInitialIndexes(List<IndexDefinition> initialIndexes) {
        this.initialIndexes = initialIndexes;
    }

    /**
     * 索引定义类
     */
    public static class IndexDefinition {
        private String label;
        private String property;
        private String type = "BTREE"; // 默认索引类型

        public IndexDefinition() {}

        public IndexDefinition(String label, String property) {
            this.label = label;
            this.property = property;
        }

        // Getters and Setters
        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return String.format("IndexDefinition{label='%s', property='%s', type='%s'}", 
                    label, property, type);
        }
    }
}