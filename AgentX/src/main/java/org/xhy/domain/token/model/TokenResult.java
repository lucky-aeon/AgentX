package org.xhy.domain.token.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Token处理结果模型
 * 包含Token处理后的结果信息
 */
public class TokenResult {
    
    /**
     * 处理后保留的消息列表
     */
    private List<TokenMessage> retainedMessages;
    
    /**
     * 摘要内容，如果没有则为null
     */
    private String summary;
    
    /**
     * 策略名称
     */
    private String strategyName;
    
    /**
     * 总Token数
     */
    private int totalTokens;
    
    /**
     * 默认构造函数
     */
    public TokenResult() {
        this.retainedMessages = new ArrayList<>();
    }
    
    /**
     * 带参数的构造函数
     */
    public TokenResult(List<TokenMessage> retainedMessages, String summary, 
                      String strategyName, int totalTokens) {
        this.retainedMessages = retainedMessages != null ? retainedMessages : new ArrayList<>();
        this.summary = summary;
        this.strategyName = strategyName;
        this.totalTokens = totalTokens;
    }
    
    // Getter和Setter
    
    public List<TokenMessage> getRetainedMessages() {
        return retainedMessages;
    }
    
    public void setRetainedMessages(List<TokenMessage> retainedMessages) {
        this.retainedMessages = retainedMessages != null ? retainedMessages : new ArrayList<>();
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getStrategyName() {
        return strategyName;
    }
    
    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }
    
    public int getTotalTokens() {
        return totalTokens;
    }
    
    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }
    
    /**
     * 获取保留消息的ID列表
     * 
     * @return 消息ID列表
     */
    public List<String> getRetainedMessageIds() {
        List<String> ids = new ArrayList<>();
        if (retainedMessages != null) {
            for (TokenMessage message : retainedMessages) {
                ids.add(message.getId());
            }
        }
        return ids;
    }
} 