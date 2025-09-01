package org.xhy.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 知识图谱增强RAG配置类 支持通过配置文件调整增强RAG的各种参数
 * 
 * @author AgentX */
@Configuration
@ConfigurationProperties(prefix = "agentx.rag.kg-enhanced")
@Validated
public class KgEnhancedRagConfiguration {

    /** 是否启用知识图谱增强RAG，默认true */
    private boolean enabled = true;

    /** 默认图谱权重，范围0.0-1.0 */
    @DecimalMin(value = "0.0", message = "图谱权重不能小于0")
    @DecimalMax(value = "1.0", message = "图谱权重不能大于1")
    private double defaultGraphWeight = 0.3;

    /** 默认最大结果数量 */
    @Min(value = 1, message = "最大结果数量不能小于1")
    @Max(value = 100, message = "最大结果数量不能超过100")
    private int defaultMaxResults = 15;

    /** 默认最小相似度阈值 */
    @DecimalMin(value = "0.0", message = "相似度阈值不能小于0")
    @DecimalMax(value = "1.0", message = "相似度阈值不能大于1")
    private double defaultMinScore = 0.7;

    /** 默认图谱遍历深度 */
    @Min(value = 1, message = "图谱遍历深度不能小于1")
    @Max(value = 10, message = "图谱遍历深度不能超过10")
    private int defaultMaxGraphDepth = 2;

    /** 默认每个实体的最大关系数量 */
    @Min(value = 1, message = "实体关系数量不能小于1")
    @Max(value = 50, message = "实体关系数量不能超过50")
    private int defaultMaxRelationsPerEntity = 5;

    /** 是否默认启用重排序 */
    private boolean defaultEnableRerank = true;

    /** 是否默认启用查询扩展 */
    private boolean defaultEnableQueryExpansion = false;

    /** 是否默认包含纯图谱结果 */
    private boolean defaultIncludeGraphOnlyResults = false;

    /** 实体提取配置 */
    private EntityExtraction entityExtraction = new EntityExtraction();

    /** 融合策略配置 */
    private FusionStrategy fusionStrategy = new FusionStrategy();

    /** 性能配置 */
    private Performance performance = new Performance();

    /** 实体提取配置 */
    public static class EntityExtraction {
        /** 默认实体提取策略 */
        private String defaultStrategy = "KEYWORD";

        /** 实体提取超时时间（毫秒） */
        @Min(value = 100, message = "实体提取超时时间不能小于100毫秒")
        @Max(value = 30000, message = "实体提取超时时间不能超过30秒")
        private long timeoutMs = 5000;

        /** 最大提取实体数量 */
        @Min(value = 1, message = "最大提取实体数量不能小于1")
        @Max(value = 100, message = "最大提取实体数量不能超过100")
        private int maxEntities = 20;

        /** 实体置信度阈值 */
        @DecimalMin(value = "0.0", message = "实体置信度阈值不能小于0")
        @DecimalMax(value = "1.0", message = "实体置信度阈值不能大于1")
        private double confidenceThreshold = 0.6;

        // Getters and Setters
        public String getDefaultStrategy() {
            return defaultStrategy;
        }

        public void setDefaultStrategy(String defaultStrategy) {
            this.defaultStrategy = defaultStrategy;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public int getMaxEntities() {
            return maxEntities;
        }

        public void setMaxEntities(int maxEntities) {
            this.maxEntities = maxEntities;
        }

        public double getConfidenceThreshold() {
            return confidenceThreshold;
        }

        public void setConfidenceThreshold(double confidenceThreshold) {
            this.confidenceThreshold = confidenceThreshold;
        }
    }

    /** 融合策略配置 */
    public static class FusionStrategy {
        /** 默认融合策略 */
        private String defaultStrategy = "ADAPTIVE_FUSION";

        /** 线性加权融合的向量权重 */
        @DecimalMin(value = "0.0", message = "向量权重不能小于0")
        @DecimalMax(value = "1.0", message = "向量权重不能大于1")
        private double vectorWeight = 0.7;

        /** RRF融合的K值 */
        @Min(value = 1, message = "RRF K值不能小于1")
        @Max(value = 1000, message = "RRF K值不能超过1000")
        private int rrfK = 60;

        /** 多样性过滤阈值 */
        @DecimalMin(value = "0.1", message = "多样性过滤阈值不能小于0.1")
        @DecimalMax(value = "1.0", message = "多样性过滤阈值不能大于1.0")
        private double diversityThreshold = 0.8;

        // Getters and Setters
        public String getDefaultStrategy() {
            return defaultStrategy;
        }

        public void setDefaultStrategy(String defaultStrategy) {
            this.defaultStrategy = defaultStrategy;
        }

        public double getVectorWeight() {
            return vectorWeight;
        }

        public void setVectorWeight(double vectorWeight) {
            this.vectorWeight = vectorWeight;
        }

        public int getRrfK() {
            return rrfK;
        }

        public void setRrfK(int rrfK) {
            this.rrfK = rrfK;
        }

        public double getDiversityThreshold() {
            return diversityThreshold;
        }

        public void setDiversityThreshold(double diversityThreshold) {
            this.diversityThreshold = diversityThreshold;
        }
    }

    /** 性能配置 */
    public static class Performance {
        /** 向量搜索超时时间（毫秒） */
        @Min(value = 1000, message = "向量搜索超时时间不能小于1秒")
        @Max(value = 60000, message = "向量搜索超时时间不能超过60秒")
        private long vectorSearchTimeoutMs = 10000;

        /** 图谱查询超时时间（毫秒） */
        @Min(value = 1000, message = "图谱查询超时时间不能小于1秒")
        @Max(value = 60000, message = "图谱查询超时时间不能超过60秒")
        private long graphQueryTimeoutMs = 15000;

        /** 最大并发查询数 */
        @Min(value = 1, message = "最大并发查询数不能小于1")
        @Max(value = 100, message = "最大并发查询数不能超过100")
        private int maxConcurrentQueries = 10;

        /** 是否启用查询缓存 */
        private boolean enableQueryCache = true;

        /** 查询缓存过期时间（分钟） */
        @Min(value = 1, message = "查询缓存过期时间不能小于1分钟")
        @Max(value = 1440, message = "查询缓存过期时间不能超过24小时")
        private int queryCacheExpirationMinutes = 30;

        // Getters and Setters
        public long getVectorSearchTimeoutMs() {
            return vectorSearchTimeoutMs;
        }

        public void setVectorSearchTimeoutMs(long vectorSearchTimeoutMs) {
            this.vectorSearchTimeoutMs = vectorSearchTimeoutMs;
        }

        public long getGraphQueryTimeoutMs() {
            return graphQueryTimeoutMs;
        }

        public void setGraphQueryTimeoutMs(long graphQueryTimeoutMs) {
            this.graphQueryTimeoutMs = graphQueryTimeoutMs;
        }

        public int getMaxConcurrentQueries() {
            return maxConcurrentQueries;
        }

        public void setMaxConcurrentQueries(int maxConcurrentQueries) {
            this.maxConcurrentQueries = maxConcurrentQueries;
        }

        public boolean isEnableQueryCache() {
            return enableQueryCache;
        }

        public void setEnableQueryCache(boolean enableQueryCache) {
            this.enableQueryCache = enableQueryCache;
        }

        public int getQueryCacheExpirationMinutes() {
            return queryCacheExpirationMinutes;
        }

        public void setQueryCacheExpirationMinutes(int queryCacheExpirationMinutes) {
            this.queryCacheExpirationMinutes = queryCacheExpirationMinutes;
        }
    }

    // Main class Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getDefaultGraphWeight() {
        return defaultGraphWeight;
    }

    public void setDefaultGraphWeight(double defaultGraphWeight) {
        this.defaultGraphWeight = defaultGraphWeight;
    }

    public int getDefaultMaxResults() {
        return defaultMaxResults;
    }

    public void setDefaultMaxResults(int defaultMaxResults) {
        this.defaultMaxResults = defaultMaxResults;
    }

    public double getDefaultMinScore() {
        return defaultMinScore;
    }

    public void setDefaultMinScore(double defaultMinScore) {
        this.defaultMinScore = defaultMinScore;
    }

    public int getDefaultMaxGraphDepth() {
        return defaultMaxGraphDepth;
    }

    public void setDefaultMaxGraphDepth(int defaultMaxGraphDepth) {
        this.defaultMaxGraphDepth = defaultMaxGraphDepth;
    }

    public int getDefaultMaxRelationsPerEntity() {
        return defaultMaxRelationsPerEntity;
    }

    public void setDefaultMaxRelationsPerEntity(int defaultMaxRelationsPerEntity) {
        this.defaultMaxRelationsPerEntity = defaultMaxRelationsPerEntity;
    }

    public boolean isDefaultEnableRerank() {
        return defaultEnableRerank;
    }

    public void setDefaultEnableRerank(boolean defaultEnableRerank) {
        this.defaultEnableRerank = defaultEnableRerank;
    }

    public boolean isDefaultEnableQueryExpansion() {
        return defaultEnableQueryExpansion;
    }

    public void setDefaultEnableQueryExpansion(boolean defaultEnableQueryExpansion) {
        this.defaultEnableQueryExpansion = defaultEnableQueryExpansion;
    }

    public boolean isDefaultIncludeGraphOnlyResults() {
        return defaultIncludeGraphOnlyResults;
    }

    public void setDefaultIncludeGraphOnlyResults(boolean defaultIncludeGraphOnlyResults) {
        this.defaultIncludeGraphOnlyResults = defaultIncludeGraphOnlyResults;
    }

    public EntityExtraction getEntityExtraction() {
        return entityExtraction;
    }

    public void setEntityExtraction(EntityExtraction entityExtraction) {
        this.entityExtraction = entityExtraction;
    }

    public FusionStrategy getFusionStrategy() {
        return fusionStrategy;
    }

    public void setFusionStrategy(FusionStrategy fusionStrategy) {
        this.fusionStrategy = fusionStrategy;
    }

    public Performance getPerformance() {
        return performance;
    }

    public void setPerformance(Performance performance) {
        this.performance = performance;
    }

    @Override
    public String toString() {
        return "KgEnhancedRagConfiguration{" + "enabled=" + enabled + ", defaultGraphWeight=" + defaultGraphWeight
                + ", defaultMaxResults=" + defaultMaxResults + ", defaultMinScore=" + defaultMinScore
                + ", defaultMaxGraphDepth=" + defaultMaxGraphDepth + ", defaultMaxRelationsPerEntity="
                + defaultMaxRelationsPerEntity + '}';
    }
}
