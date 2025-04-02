package org.xhy.domain.agent.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Agent模型配置类，用于表示大语言模型的相关配置参数
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LLMModelConfig {
    
    /**
     * 模型id
     */
    private String modelId;
    
    /**
     * 温度参数，范围0-2，值越大创造性越强，越小则越保守
     */
    private Double temperature = 0.7;
    
    /**
     * Top P参数，范围0-1，控制输出的多样性
     */
    private Double topP = 0.7;

    private Integer topK = 50;


    /**
     * 最大Token数，适用于滑动窗口和摘要策略
     */
    private Integer maxTokens;
    /**
     * 策略类型 @link TokenOverflowStrategyEnum
     */
    private String strategyType;
    /**
     * 预留缓冲比例，适用于滑动窗口策略
     * 范围0-1之间的小数，表示预留的空间比例
     */
    private Double reserveRatio;
    /**
     * 摘要触发阈值（消息数量），适用于摘要策略
     */
    private Integer summaryThreshold;

    /**
     * 无参构造函数
     */
    public LLMModelConfig() {
    }


    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public String getModelId() {
        return modelId;
    }
    
    public void setModelId(String modelId) {
        this.modelId = modelId;
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

    public String getStrategyType() {
        return strategyType;
    }

    public void setStrategyType(String strategyType) {
        this.strategyType = strategyType;
    }

    public Double getReserveRatio() {
        return reserveRatio;
    }

    public void setReserveRatio(Double reserveRatio) {
        this.reserveRatio = reserveRatio;
    }

    public Integer getSummaryThreshold() {
        return summaryThreshold;
    }

    public void setSummaryThreshold(Integer summaryThreshold) {
        this.summaryThreshold = summaryThreshold;
    }
}