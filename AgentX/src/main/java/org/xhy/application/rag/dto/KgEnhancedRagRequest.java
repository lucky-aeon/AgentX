package org.xhy.application.rag.dto;

import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * 知识图谱增强RAG检索请求DTO
 * 
 * @author AgentX
 */
public class KgEnhancedRagRequest {

    /** 数据集ID列表 */
    @NotEmpty(message = "数据集ID列表不能为空")
    @Size(max = 20, message = "数据集ID列表不能超过20个")
    private List<String> datasetIds;

    /** 搜索问题 */
    @NotBlank(message = "搜索问题不能为空")
    @Size(min = 1, max = 1000, message = "搜索问题长度必须在1-1000字符之间")
    private String question;

    /** 最大返回结果数量，默认15 */
    @Min(value = 1, message = "最大返回结果数量不能小于1")
    @Max(value = 100, message = "最大返回结果数量不能超过100")
    private Integer maxResults = 15;

    /** 最小相似度阈值，默认0.7 */
    @DecimalMin(value = "0.0", message = "相似度阈值不能小于0")
    @DecimalMax(value = "1.0", message = "相似度阈值不能大于1")
    private Double minScore = 0.7;

    /** 是否启用重排序，默认true */
    private Boolean enableRerank = true;

    /** 搜索候选结果倍数，默认2倍用于重排序 */
    @Min(value = 1, message = "候选结果倍数不能小于1")
    @Max(value = 5, message = "候选结果倍数不能超过5")
    private Integer candidateMultiplier = 2;

    /** 是否启用查询扩展，默认false */
    private Boolean enableQueryExpansion = false;

    /** 是否启用知识图谱增强，默认true */
    private Boolean enableGraphEnhancement = true;

    /** 是否包含纯图谱结果，默认false */
    private Boolean includeGraphOnlyResults = false;

    /** 图谱查询权重，默认0.3 */
    @DecimalMin(value = "0.0", message = "图谱查询权重不能小于0")
    @DecimalMax(value = "1.0", message = "图谱查询权重不能大于1")
    private Double graphWeight = 0.3;

    /** 实体识别策略，默认KEYWORD */
    private EntityExtractionStrategy entityExtractionStrategy = EntityExtractionStrategy.KEYWORD;

    /** 图谱遍历最大深度，默认2 */
    @Min(value = 1, message = "图谱遍历深度不能小于1")
    @Max(value = 5, message = "图谱遍历深度不能超过5")
    private Integer maxGraphDepth = 2;

    /** 每个实体的最大关系数量，默认5 */
    @Min(value = 1, message = "实体关系数量不能小于1")
    @Max(value = 20, message = "实体关系数量不能超过20")
    private Integer maxRelationsPerEntity = 5;

    /**
     * 实体提取策略枚举
     */
    public enum EntityExtractionStrategy {
        /** 关键词匹配 */
        KEYWORD,
        /** NLP命名实体识别 */
        NER,
        /** LLM实体提取 */
        LLM
    }

    // Getters and Setters
    public List<String> getDatasetIds() {
        return datasetIds;
    }

    public void setDatasetIds(List<String> datasetIds) {
        this.datasetIds = datasetIds;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Integer getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }

    public Double getMinScore() {
        return minScore;
    }

    public void setMinScore(Double minScore) {
        this.minScore = minScore;
    }

    public Boolean getEnableRerank() {
        return enableRerank;
    }

    public void setEnableRerank(Boolean enableRerank) {
        this.enableRerank = enableRerank;
    }

    public Integer getCandidateMultiplier() {
        return candidateMultiplier;
    }

    public void setCandidateMultiplier(Integer candidateMultiplier) {
        this.candidateMultiplier = candidateMultiplier;
    }

    public Boolean getEnableQueryExpansion() {
        return enableQueryExpansion;
    }

    public void setEnableQueryExpansion(Boolean enableQueryExpansion) {
        this.enableQueryExpansion = enableQueryExpansion;
    }

    public Boolean getEnableGraphEnhancement() {
        return enableGraphEnhancement;
    }

    public void setEnableGraphEnhancement(Boolean enableGraphEnhancement) {
        this.enableGraphEnhancement = enableGraphEnhancement;
    }

    public Boolean getIncludeGraphOnlyResults() {
        return includeGraphOnlyResults;
    }

    public void setIncludeGraphOnlyResults(Boolean includeGraphOnlyResults) {
        this.includeGraphOnlyResults = includeGraphOnlyResults;
    }

    public Double getGraphWeight() {
        return graphWeight;
    }

    public void setGraphWeight(Double graphWeight) {
        this.graphWeight = graphWeight;
    }

    public EntityExtractionStrategy getEntityExtractionStrategy() {
        return entityExtractionStrategy;
    }

    public void setEntityExtractionStrategy(EntityExtractionStrategy entityExtractionStrategy) {
        this.entityExtractionStrategy = entityExtractionStrategy;
    }

    public Integer getMaxGraphDepth() {
        return maxGraphDepth;
    }

    public void setMaxGraphDepth(Integer maxGraphDepth) {
        this.maxGraphDepth = maxGraphDepth;
    }

    public Integer getMaxRelationsPerEntity() {
        return maxRelationsPerEntity;
    }

    public void setMaxRelationsPerEntity(Integer maxRelationsPerEntity) {
        this.maxRelationsPerEntity = maxRelationsPerEntity;
    }

    @Override
    public String toString() {
        return "KgEnhancedRagRequest{" +
                "datasetIds=" + datasetIds +
                ", question='" + question + '\'' +
                ", maxResults=" + maxResults +
                ", minScore=" + minScore +
                ", enableGraphEnhancement=" + enableGraphEnhancement +
                ", graphWeight=" + graphWeight +
                ", entityExtractionStrategy=" + entityExtractionStrategy +
                '}';
    }
}
