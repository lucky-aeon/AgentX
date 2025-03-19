package org.xhy.domain.llm.model;

/**
 * LLM响应模型
 */
public class LlmResponse {
    
    /**
     * 响应文本内容
     */
    private String content;
    
    /**
     * 服务商名称
     */
    private String provider;
    
    /**
     * 模型名称
     */
    private String model;
    
    /**
     * 完成原因
     */
    private String finishReason;
    
    /**
     * 使用的token数量
     */
    private Integer tokenUsage;
    
    public LlmResponse() {
    }
    
    public LlmResponse(String content) {
        this.content = content;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getFinishReason() {
        return finishReason;
    }
    
    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }
    
    public Integer getTokenUsage() {
        return tokenUsage;
    }
    
    public void setTokenUsage(Integer tokenUsage) {
        this.tokenUsage = tokenUsage;
    }
}
