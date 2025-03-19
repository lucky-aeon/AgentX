package org.xhy.domain.llm.model;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM请求模型
 */
public class LlmRequest {
    
    /**
     * 模型名称
     */
    private String model;
    
    /**
     * 消息列表
     */
    private List<LlmMessage> messages;
    
    /**
     * 温度参数 (控制随机性)
     */
    private Double temperature;
    
    /**
     * 最大生成token数
     */
    private Integer maxTokens;
    
    /**
     * 是否流式响应
     */
    private Boolean stream;
    
    public LlmRequest() {
        this.messages = new ArrayList<>();
        this.temperature = 0.7;
        this.stream = false;
    }

    /**
     * 添加消息
     *
     * @param message 消息
     * @return this
     */
    public LlmRequest addMessage(LlmMessage message) {
        this.messages.add(message);
        return this;
    }

    /**
     * 添加用户消息
     *
     * @param content 消息内容
     * @return this
     */
    public LlmRequest addUserMessage(String content) {
        return addMessage(LlmMessage.ofUser(content));
    }

    /**
     * 添加系统消息
     *
     * @param content 消息内容
     * @return this
     */
    public LlmRequest addSystemMessage(String content) {
        return addMessage(LlmMessage.ofSystem(content));
    }

    /**
     * 添加助手消息
     *
     * @param content 消息内容
     * @return this
     */
    public LlmRequest addAssistantMessage(String content) {
        return addMessage(LlmMessage.ofAssistant(content));
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public List<LlmMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<LlmMessage> messages) {
        this.messages = messages;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public Boolean getStream() {
        return stream;
    }
    
    public void setStream(Boolean stream) {
        this.stream = stream;
    }
}
