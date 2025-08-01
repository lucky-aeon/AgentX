package org.xhy.application.trace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agent执行链路详细记录DTO
 */
public class AgentExecutionDetailDTO {
    
    /** 追踪ID */
    private String traceId;
    
    /** 执行序号 */
    private Integer sequenceNo;
    
    /** 步骤类型 */
    private String stepType;
    
    /** 步骤类型描述 */
    private String stepTypeDescription;
    
    /** 用户发送的消息内容 */
    private String userMessage;
    
    /** 用户消息类型 */
    private String userMessageType;
    
    /** 大模型回复的消息内容 */
    private String aiResponse;
    
    /** AI响应类型 */
    private String aiResponseType;
    
    /** 此次使用的模型ID */
    private String modelId;
    
    /** 提供商名称 */
    private String providerName;
    
    /** 输入Token数 */
    private Integer inputTokens;
    
    /** 输出Token数 */
    private Integer outputTokens;
    
    /** 模型调用耗时(毫秒) */
    private Integer modelCallTime;
    
    /** 工具名称 */
    private String toolName;
    
    /** 工具调用入参 */
    private String toolRequestArgs;
    
    /** 工具调用出参 */
    private String toolResponseData;
    
    /** 工具执行耗时(毫秒) */
    private Integer toolExecutionTime;
    
    /** 工具执行是否成功 */
    private Boolean toolSuccess;
    
    /** 是否触发了降级 */
    private Boolean isFallbackUsed;
    
    /** 降级原因 */
    private String fallbackReason;
    
    /** 降级前的模型 */
    private String fallbackFromModel;
    
    /** 降级后的模型 */
    private String fallbackToModel;
    
    /** 步骤成本 */
    private BigDecimal stepCost;
    
    /** 步骤执行是否成功 */
    private Boolean stepSuccess;
    
    /** 步骤错误信息 */
    private String stepErrorMessage;
    
    /** 创建时间 */
    private LocalDateTime createdTime;

    public AgentExecutionDetailDTO() {
    }

    // Getter和Setter方法
    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Integer getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Integer sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public String getStepType() {
        return stepType;
    }

    public void setStepType(String stepType) {
        this.stepType = stepType;
    }

    public String getStepTypeDescription() {
        return stepTypeDescription;
    }

    public void setStepTypeDescription(String stepTypeDescription) {
        this.stepTypeDescription = stepTypeDescription;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getUserMessageType() {
        return userMessageType;
    }

    public void setUserMessageType(String userMessageType) {
        this.userMessageType = userMessageType;
    }

    public String getAiResponse() {
        return aiResponse;
    }

    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }

    public String getAiResponseType() {
        return aiResponseType;
    }

    public void setAiResponseType(String aiResponseType) {
        this.aiResponseType = aiResponseType;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(Integer inputTokens) {
        this.inputTokens = inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(Integer outputTokens) {
        this.outputTokens = outputTokens;
    }

    public Integer getModelCallTime() {
        return modelCallTime;
    }

    public void setModelCallTime(Integer modelCallTime) {
        this.modelCallTime = modelCallTime;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolRequestArgs() {
        return toolRequestArgs;
    }

    public void setToolRequestArgs(String toolRequestArgs) {
        this.toolRequestArgs = toolRequestArgs;
    }

    public String getToolResponseData() {
        return toolResponseData;
    }

    public void setToolResponseData(String toolResponseData) {
        this.toolResponseData = toolResponseData;
    }

    public Integer getToolExecutionTime() {
        return toolExecutionTime;
    }

    public void setToolExecutionTime(Integer toolExecutionTime) {
        this.toolExecutionTime = toolExecutionTime;
    }

    public Boolean getToolSuccess() {
        return toolSuccess;
    }

    public void setToolSuccess(Boolean toolSuccess) {
        this.toolSuccess = toolSuccess;
    }

    public Boolean getIsFallbackUsed() {
        return isFallbackUsed;
    }

    public void setIsFallbackUsed(Boolean isFallbackUsed) {
        this.isFallbackUsed = isFallbackUsed;
    }

    public String getFallbackReason() {
        return fallbackReason;
    }

    public void setFallbackReason(String fallbackReason) {
        this.fallbackReason = fallbackReason;
    }

    public String getFallbackFromModel() {
        return fallbackFromModel;
    }

    public void setFallbackFromModel(String fallbackFromModel) {
        this.fallbackFromModel = fallbackFromModel;
    }

    public String getFallbackToModel() {
        return fallbackToModel;
    }

    public void setFallbackToModel(String fallbackToModel) {
        this.fallbackToModel = fallbackToModel;
    }

    public BigDecimal getStepCost() {
        return stepCost;
    }

    public void setStepCost(BigDecimal stepCost) {
        this.stepCost = stepCost;
    }

    public Boolean getStepSuccess() {
        return stepSuccess;
    }

    public void setStepSuccess(Boolean stepSuccess) {
        this.stepSuccess = stepSuccess;
    }

    public String getStepErrorMessage() {
        return stepErrorMessage;
    }

    public void setStepErrorMessage(String stepErrorMessage) {
        this.stepErrorMessage = stepErrorMessage;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
}