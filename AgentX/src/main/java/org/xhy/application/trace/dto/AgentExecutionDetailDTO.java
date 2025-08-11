package org.xhy.application.trace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Agent执行链路详细记录DTO */
public class AgentExecutionDetailDTO {

    /** 追踪ID */
    private String traceId;

    /** 统一的消息内容 */
    private String messageContent;

    /** 消息类型 */
    private String messageType;

    /** 此次使用的模型ID */
    private String modelId;

    /** 提供商名称 */
    private String providerName;

    /** 消息Token数 */
    private Integer messageTokens;

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

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
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

    public Integer getMessageTokens() {
        return messageTokens;
    }

    public void setMessageTokens(Integer messageTokens) {
        this.messageTokens = messageTokens;
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