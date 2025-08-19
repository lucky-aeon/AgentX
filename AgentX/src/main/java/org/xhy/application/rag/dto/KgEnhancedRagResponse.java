package org.xhy.application.rag.dto;

import java.util.List;

import org.xhy.application.knowledgeGraph.dto.GraphQueryResponse;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 知识图谱增强RAG检索响应DTO
 * 
 * @author AgentX
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KgEnhancedRagResponse {

    /** 是否成功 */
    private boolean success;

    /** 错误消息 */
    private String errorMessage;

    /** 增强搜索结果列表 */
    private List<EnhancedResult> results;

    /** 向量搜索结果数量 */
    private int vectorResultCount;

    /** 图谱实体数量 */
    private int graphEntityCount;

    /** 图谱关系数量 */
    private int graphRelationshipCount;

    /** 处理时间（毫秒） */
    private long processingTimeMs;

    /** 搜索统计信息 */
    private SearchStatistics statistics;

    /**
     * 增强搜索结果
     */
    public static class EnhancedResult {
        /** 文档单元（来自向量搜索） */
        private DocumentUnitDTO documentUnit;

        /** 结果来源类型 */
        private String sourceType; // VECTOR, GRAPH, HYBRID

        /** 综合相关性评分 */
        private double relevanceScore;

        /** 向量相似度评分 */
        private Double vectorScore;

        /** 图谱相关性评分 */
        private Double graphScore;

        /** 关联的图谱实体 */
        private List<GraphQueryResponse.NodeResult> graphEntities;

        /** 关联的图谱关系 */
        private List<GraphQueryResponse.RelationshipResult> graphRelationships;

        /** 增强信息摘要 */
        private String enhancementSummary;

        // Getters and Setters
        public DocumentUnitDTO getDocumentUnit() {
            return documentUnit;
        }

        public void setDocumentUnit(DocumentUnitDTO documentUnit) {
            this.documentUnit = documentUnit;
        }

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public double getRelevanceScore() {
            return relevanceScore;
        }

        public void setRelevanceScore(double relevanceScore) {
            this.relevanceScore = relevanceScore;
        }

        public Double getVectorScore() {
            return vectorScore;
        }

        public void setVectorScore(Double vectorScore) {
            this.vectorScore = vectorScore;
        }

        public Double getGraphScore() {
            return graphScore;
        }

        public void setGraphScore(Double graphScore) {
            this.graphScore = graphScore;
        }

        public List<GraphQueryResponse.NodeResult> getGraphEntities() {
            return graphEntities;
        }

        public void setGraphEntities(List<GraphQueryResponse.NodeResult> graphEntities) {
            this.graphEntities = graphEntities;
        }

        public List<GraphQueryResponse.RelationshipResult> getGraphRelationships() {
            return graphRelationships;
        }

        public void setGraphRelationships(List<GraphQueryResponse.RelationshipResult> graphRelationships) {
            this.graphRelationships = graphRelationships;
        }

        public String getEnhancementSummary() {
            return enhancementSummary;
        }

        public void setEnhancementSummary(String enhancementSummary) {
            this.enhancementSummary = enhancementSummary;
        }
    }

    /**
     * 搜索统计信息
     */
    public static class SearchStatistics {
        /** 总查询时间 */
        private long totalQueryTime;

        /** 向量搜索时间 */
        private long vectorSearchTime;

        /** 图谱查询时间 */
        private long graphQueryTime;

        /** 结果融合时间 */
        private long fusionTime;

        /** 重排序时间 */
        private long rerankTime;

        /** 实体提取数量 */
        private int extractedEntitiesCount;

        /** 图谱查询次数 */
        private int graphQueryCount;

        // Getters and Setters
        public long getTotalQueryTime() {
            return totalQueryTime;
        }

        public void setTotalQueryTime(long totalQueryTime) {
            this.totalQueryTime = totalQueryTime;
        }

        public long getVectorSearchTime() {
            return vectorSearchTime;
        }

        public void setVectorSearchTime(long vectorSearchTime) {
            this.vectorSearchTime = vectorSearchTime;
        }

        public long getGraphQueryTime() {
            return graphQueryTime;
        }

        public void setGraphQueryTime(long graphQueryTime) {
            this.graphQueryTime = graphQueryTime;
        }

        public long getFusionTime() {
            return fusionTime;
        }

        public void setFusionTime(long fusionTime) {
            this.fusionTime = fusionTime;
        }

        public long getRerankTime() {
            return rerankTime;
        }

        public void setRerankTime(long rerankTime) {
            this.rerankTime = rerankTime;
        }

        public int getExtractedEntitiesCount() {
            return extractedEntitiesCount;
        }

        public void setExtractedEntitiesCount(int extractedEntitiesCount) {
            this.extractedEntitiesCount = extractedEntitiesCount;
        }

        public int getGraphQueryCount() {
            return graphQueryCount;
        }

        public void setGraphQueryCount(int graphQueryCount) {
            this.graphQueryCount = graphQueryCount;
        }
    }

    // Main class Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<EnhancedResult> getResults() {
        return results;
    }

    public void setResults(List<EnhancedResult> results) {
        this.results = results;
    }

    public int getVectorResultCount() {
        return vectorResultCount;
    }

    public void setVectorResultCount(int vectorResultCount) {
        this.vectorResultCount = vectorResultCount;
    }

    public int getGraphEntityCount() {
        return graphEntityCount;
    }

    public void setGraphEntityCount(int graphEntityCount) {
        this.graphEntityCount = graphEntityCount;
    }

    public int getGraphRelationshipCount() {
        return graphRelationshipCount;
    }

    public void setGraphRelationshipCount(int graphRelationshipCount) {
        this.graphRelationshipCount = graphRelationshipCount;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public SearchStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(SearchStatistics statistics) {
        this.statistics = statistics;
    }

    /**
     * 创建成功响应
     */
    public static KgEnhancedRagResponse success(List<EnhancedResult> results) {
        KgEnhancedRagResponse response = new KgEnhancedRagResponse();
        response.setSuccess(true);
        response.setResults(results);
        return response;
    }

    /**
     * 创建失败响应
     */
    public static KgEnhancedRagResponse failure(String errorMessage) {
        KgEnhancedRagResponse response = new KgEnhancedRagResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }

    @Override
    public String toString() {
        return "KgEnhancedRagResponse{" +
                "success=" + success +
                ", resultCount=" + (results != null ? results.size() : 0) +
                ", vectorResultCount=" + vectorResultCount +
                ", graphEntityCount=" + graphEntityCount +
                ", processingTimeMs=" + processingTimeMs +
                '}';
    }
}
