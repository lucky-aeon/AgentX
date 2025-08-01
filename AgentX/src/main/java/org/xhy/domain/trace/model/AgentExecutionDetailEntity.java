package org.xhy.domain.trace.model;

import com.baomidou.mybatisplus.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agent执行链路详细记录实体
 * 记录Agent执行过程中每个步骤的详细信息
 */
@TableName("agent_execution_details")
public class AgentExecutionDetailEntity {

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联汇总表的追踪ID */
    @TableField("trace_id")
    private String traceId;

    /** 执行序号，同一trace_id内递增 */
    @TableField("sequence_no")
    private Integer sequenceNo;

    /** 步骤类型：USER_MESSAGE, AI_RESPONSE, TOOL_CALL */
    @TableField("step_type")
    private String stepType;

    /** 用户发送的消息内容 */
    @TableField("user_message")
    private String userMessage;

    /** 用户消息类型 */
    @TableField("user_message_type")
    private String userMessageType;

    /** 大模型回复的消息内容 */
    @TableField("ai_response")
    private String aiResponse;

    /** AI响应类型 */
    @TableField("ai_response_type")
    private String aiResponseType;

    /** 此次使用的模型ID */
    @TableField("model_id")
    private String modelId;

    /** 提供商名称 */
    @TableField("provider_name")
    private String providerName;

    /** 输入Token数 */
    @TableField("input_tokens")
    private Integer inputTokens;

    /** 输出Token数 */
    @TableField("output_tokens")
    private Integer outputTokens;

    /** 模型调用耗时(毫秒) */
    @TableField("model_call_time")
    private Integer modelCallTime;

    /** 工具名称 */
    @TableField("tool_name")
    private String toolName;

    /** 工具调用入参(JSON格式) */
    @TableField("tool_request_args")
    private String toolRequestArgs;

    /** 工具调用出参(JSON格式) */
    @TableField("tool_response_data")
    private String toolResponseData;

    /** 工具执行耗时(毫秒) */
    @TableField("tool_execution_time")
    private Integer toolExecutionTime;

    /** 工具执行是否成功 */
    @TableField("tool_success")
    private Boolean toolSuccess;

    /** 是否触发了平替/降级 */
    @TableField("is_fallback_used")
    private Boolean isFallbackUsed;

    /** 降级原因 */
    @TableField("fallback_reason")
    private String fallbackReason;

    /** 降级前的模型 */
    @TableField("fallback_from_model")
    private String fallbackFromModel;

    /** 降级后的模型 */
    @TableField("fallback_to_model")
    private String fallbackToModel;

    /** 步骤成本 */
    @TableField("step_cost")
    private BigDecimal stepCost;

    /** 步骤执行是否成功 */
    @TableField("step_success")
    private Boolean stepSuccess;

    /** 步骤错误信息 */
    @TableField("step_error_message")
    private String stepErrorMessage;

    /** 创建时间 */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    public AgentExecutionDetailEntity() {
        this.isFallbackUsed = false;
        this.stepSuccess = true;
        this.stepCost = BigDecimal.ZERO;
    }

    /** 创建用户消息步骤 */
    public static AgentExecutionDetailEntity createUserMessageStep(String traceId, Integer sequenceNo, 
            String userMessage, String messageType) {
        AgentExecutionDetailEntity entity = new AgentExecutionDetailEntity();
        entity.setTraceId(traceId);
        entity.setSequenceNo(sequenceNo);
        entity.setStepType("USER_MESSAGE");
        entity.setUserMessage(userMessage);
        entity.setUserMessageType(messageType);
        return entity;
    }

    /** 创建AI响应步骤 */
    public static AgentExecutionDetailEntity createAiResponseStep(String traceId, Integer sequenceNo,
            String aiResponse, String modelId, String providerName, Integer inputTokens, Integer outputTokens,
            Integer modelCallTime, BigDecimal stepCost) {
        AgentExecutionDetailEntity entity = new AgentExecutionDetailEntity();
        entity.setTraceId(traceId);
        entity.setSequenceNo(sequenceNo);
        entity.setStepType("AI_RESPONSE");
        entity.setAiResponse(aiResponse);
        entity.setModelId(modelId);
        entity.setProviderName(providerName);
        entity.setInputTokens(inputTokens);
        entity.setOutputTokens(outputTokens);
        entity.setModelCallTime(modelCallTime);
        entity.setStepCost(stepCost);
        return entity;
    }

    /** 创建工具调用步骤 */
    public static AgentExecutionDetailEntity createToolCallStep(String traceId, Integer sequenceNo,
            String toolName, String requestArgs, String responseData, Integer executionTime, Boolean success) {
        AgentExecutionDetailEntity entity = new AgentExecutionDetailEntity();
        entity.setTraceId(traceId);
        entity.setSequenceNo(sequenceNo);
        entity.setStepType("TOOL_CALL");
        entity.setToolName(toolName);
        entity.setToolRequestArgs(requestArgs);
        entity.setToolResponseData(responseData);
        entity.setToolExecutionTime(executionTime);
        entity.setToolSuccess(success);
        entity.setStepSuccess(success);
        return entity;
    }

    /** 设置模型降级信息 */
    public void setFallbackInfo(String reason, String fromModel, String toModel) {
        this.isFallbackUsed = true;
        this.fallbackReason = reason;
        this.fallbackFromModel = fromModel;
        this.fallbackToModel = toModel;
    }

    /** 标记步骤失败 */
    public void markStepFailed(String errorMessage) {
        this.stepSuccess = false;
        this.stepErrorMessage = errorMessage;
    }

    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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