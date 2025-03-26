package org.xhy.domain.token.model;

import java.util.List;

/**
 * Token处理结果
 */
public class TokenProcessResult {
    /**
     * 处理后保留的消息列表
     */
    private List<TokenMessage> retainedMessages;
    
    /**
     * 被移除消息的摘要（如果有的话）
     */
    private String summary;
    
    /**
     * 处理后的总token数
     */
    private int totalTokens;
    
    /**
     * 使用的策略名称
     */
    private String strategyName;
    
    /**
     * 是否进行了处理
     * true: 消息被处理过（如被截断、摘要等）
     * false: 消息未经处理（原样返回）
     */
    private boolean processed;

    // Getters and Setters
    public List<TokenMessage> getRetainedMessages() {
        return retainedMessages;
    }

    public void setRetainedMessages(List<TokenMessage> retainedMessages) {
        this.retainedMessages = retainedMessages;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }
} 