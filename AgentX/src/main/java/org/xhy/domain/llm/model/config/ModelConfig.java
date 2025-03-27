package org.xhy.domain.llm.model.config;

/**
 * 模型配置
 */
public class ModelConfig {
    
    private Integer maxContextLength;
    private Double temperature;
    private Double topP;
    private Integer maxTokens;
    private Double frequencyPenalty;
    private Double presencePenalty;
    private String[] stopSequences;
    
    public Integer getMaxContextLength() {
        return maxContextLength;
    }
    
    public void setMaxContextLength(Integer maxContextLength) {
        this.maxContextLength = maxContextLength;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    public Double getTopP() {
        return topP;
    }
    
    public void setTopP(Double topP) {
        this.topP = topP;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }
    
    public void setFrequencyPenalty(Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }
    
    public Double getPresencePenalty() {
        return presencePenalty;
    }
    
    public void setPresencePenalty(Double presencePenalty) {
        this.presencePenalty = presencePenalty;
    }
    
    public String[] getStopSequences() {
        return stopSequences;
    }
    
    public void setStopSequences(String[] stopSequences) {
        this.stopSequences = stopSequences;
    }
} 