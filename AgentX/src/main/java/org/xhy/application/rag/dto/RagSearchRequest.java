package org.xhy.application.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * RAG搜索请求DTO
 * 
 * @author shilong.zang
 */
public class RagSearchRequest {
    
    /**
     * 数据集ID列表
     */
    @NotEmpty(message = "数据集ID列表不能为空")
    @Size(max = 20, message = "数据集ID列表不能超过20个")
    private List<String> datasetIds;
    
    /**
     * 搜索问题
     */
    @NotBlank(message = "搜索问题不能为空")
    @Size(min = 1, max = 1000, message = "搜索问题长度必须在1-1000字符之间")
    private String question;
    
    /**
     * 最大返回结果数量，默认15
     */
    @Min(value = 1, message = "最大返回结果数量不能小于1")
    @Max(value = 100, message = "最大返回结果数量不能超过100")
    private Integer maxResults = 15;
    
    /**
     * 最小相似度阈值，默认0.7
     */
    @DecimalMin(value = "0.0", message = "相似度阈值不能小于0")
    @DecimalMax(value = "1.0", message = "相似度阈值不能大于1")
    private Double minScore = 0.7;
    
    /**
     * 是否启用重排序，默认true
     */
    private Boolean enableRerank = true;
    
    /**
     * 搜索候选结果倍数，默认2倍用于重排序
     */
    @Min(value = 1, message = "候选结果倍数不能小于1")
    @Max(value = 5, message = "候选结果倍数不能超过5")
    private Integer candidateMultiplier = 2;
    
    /**
     * 搜索超时时间（秒），默认30秒
     */
    @Min(value = 1, message = "搜索超时时间不能小于1秒")
    @Max(value = 300, message = "搜索超时时间不能超过300秒")
    private Integer timeoutSeconds = 30;
    
    /**
     * 是否启用查询扩展，默认false
     */
    private Boolean enableQueryExpansion = false;

    /**
     * 获取智能调整后的相似度阈值
     * 根据查询长度自动调整：短查询提高阈值，长查询降低阈值
     * @return 调整后的相似度阈值
     */
    public Double getAdjustedMinScore() {
        if (question == null || question.trim().isEmpty()) {
            return minScore;
        }
        
        int queryLength = question.trim().length();
        double adjustedScore = minScore;
        
        // 短查询（<10字符）提高阈值
        if (queryLength < 10) {
            adjustedScore = Math.min(minScore + 0.05, 1.0);
        }
        // 长查询（>50字符）降低阈值
        else if (queryLength > 50) {
            adjustedScore = Math.max(minScore - 0.05, 0.0);
        }
        
        return adjustedScore;
    }
    
    /**
     * 获取智能调整后的候选结果倍数
     * 根据重排序和结果数量自动调整
     * @return 调整后的候选结果倍数
     */
    public Integer getAdjustedCandidateMultiplier() {
        if (!enableRerank) {
            return 1; // 不重排序时使用1倍
        }
        
        // 根据请求的结果数量动态调整
        if (maxResults <= 5) {
            return Math.max(candidateMultiplier, 3); // 少量结果时增加候选数
        } else if (maxResults >= 20) {
            return Math.min(candidateMultiplier, 2); // 大量结果时减少候选数
        }
        
        return candidateMultiplier;
    }

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
    
    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Boolean getEnableQueryExpansion() {
        return enableQueryExpansion;
    }

    public void setEnableQueryExpansion(Boolean enableQueryExpansion) {
        this.enableQueryExpansion = enableQueryExpansion;
    }
}