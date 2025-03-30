package org.xhy.domain.llm.model.config;

/**
 * 模型配置
 */
public class LLMModelConfig {
    
    /**
     * 最大上下文长度
     */
    private Integer maxContextLength;

    public Integer getMaxContextLength() {
        return maxContextLength;
    }

    public void setMaxContextLength(Integer maxContextLength) {
        this.maxContextLength = maxContextLength;
    }
}