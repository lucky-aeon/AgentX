package org.xhy.application.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * RAG流式问答请求DTO
 * 
 * @author shilong.zang
 */
public class RagStreamChatRequest {
    
    /**
     * 用户问题
     */
    @NotBlank(message = "问题不能为空")
    @Size(min = 1, max = 1000, message = "问题长度必须在1-1000字符之间")
    private String question;
    
    /**
     * 数据集ID列表（当fileId为空时使用）
     */
    @Size(max = 20, message = "数据集ID列表不能超过20个")
    private List<String> datasetIds;
    
    /**
     * 指定文件ID（优先级高于datasetIds）
     */
    private String fileId;
    
    /**
     * RAG搜索相关配置
     */
    private Integer maxResults = 5;  // 默认检索5个相关文档
    private Double minScore = 0.7;   // 默认相似度阈值
    private Boolean enableRerank = true;  // 默认启用重排序

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getDatasetIds() {
        return datasetIds;
    }

    public void setDatasetIds(List<String> datasetIds) {
        this.datasetIds = datasetIds;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
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
}