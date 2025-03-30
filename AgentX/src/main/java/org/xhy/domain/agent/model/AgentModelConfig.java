package org.xhy.domain.agent.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Agent模型配置类，用于表示大语言模型的相关配置参数
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentModelConfig {
    
    /**
     * 模型名称，如：gpt-4-0125-preview, claude-3-opus-20240229等
     */
    private String modelName;
    
    /**
     * 温度参数，范围0-2，值越大创造性越强，越小则越保守
     */
    private Double temperature;
    
    /**
     * Top P参数，范围0-1，控制输出的多样性
     */
    private Double topP;
    
    /**
     * 最大令牌数，控制生成的内容长度
     */
    private Integer maxTokens;
    
    /**
     * 是否启用记忆功能
     */
    private Boolean loadMemory;
    
    /**
     * 系统消息（仅对特定模型有效）
     */
    private String systemMessage;
    
    /**
     * 无参构造函数
     */
    public AgentModelConfig() {
    }
    
    /**
     * 全参构造函数
     */
    public AgentModelConfig(String modelName, Double temperature, Double topP, Integer maxTokens, Boolean loadMemory, String systemMessage) {
        this.modelName = modelName;
        this.temperature = temperature;
        this.topP = topP;
        this.maxTokens = maxTokens;
        this.loadMemory = loadMemory;
        this.systemMessage = systemMessage;
    }
    
    /**
     * 创建默认配置
     */
    public static AgentModelConfig createDefault() {
        AgentModelConfig config = new AgentModelConfig();
        config.setModelName("gpt-3.5-turbo");
        config.setTemperature(0.7);
        config.setTopP(1.0);
        config.setMaxTokens(2000);
        config.setLoadMemory(true);
        return config;
    }
    
    // Getter和Setter方法
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
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
    
    public Boolean getLoadMemory() {
        return loadMemory;
    }
    
    public void setLoadMemory(Boolean loadMemory) {
        this.loadMemory = loadMemory;
    }
    
    public String getSystemMessage() {
        return systemMessage;
    }
    
    public void setSystemMessage(String systemMessage) {
        this.systemMessage = systemMessage;
    }
} 