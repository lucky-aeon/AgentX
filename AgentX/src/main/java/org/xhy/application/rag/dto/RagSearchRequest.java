package org.xhy.application.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
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
    private List<String> datasetIds;
    
    /**
     * 搜索问题
     */
    @NotBlank(message = "搜索问题不能为空")
    private String question;
    
    /**
     * 最大返回结果数量，默认15
     */
    @Min(value = 1, message = "最大返回结果数量不能小于1")
    private Integer maxResults = 15;

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
}