package org.xhy.domain.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.xhy.domain.trace.constant.ExecutionPhase;
import org.xhy.domain.trace.constant.ExecutionStepType;
import org.xhy.domain.trace.model.*;
import org.xhy.domain.trace.repository.AgentExecutionDetailRepository;
import org.xhy.domain.trace.repository.AgentExecutionSummaryRepository;
import org.xhy.infrastructure.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Agent执行链路追踪领域服务 负责处理追踪数据的核心业务逻辑 */
@Service
public class AgentExecutionTraceDomainService {

    private final AgentExecutionSummaryRepository summaryRepository;
    private final AgentExecutionDetailRepository detailRepository;

    public AgentExecutionTraceDomainService(AgentExecutionSummaryRepository summaryRepository,
            AgentExecutionDetailRepository detailRepository) {
        this.summaryRepository = summaryRepository;
        this.detailRepository = detailRepository;
    }

    /** 创建新的执行追踪
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param agentId Agent ID
     * @return 追踪上下文 */
    public TraceContext createTrace(String userId, String sessionId, String agentId) {
        String traceId = generateTraceId();

        // 创建汇总记录
        AgentExecutionSummaryEntity summary = AgentExecutionSummaryEntity.create(traceId, userId, sessionId, agentId);
        summaryRepository.insert(summary);

        return TraceContext.create(traceId, userId, sessionId, agentId);
    }

    /** 记录用户消息
     * 
     * @param traceContext 追踪上下文
     * @param userMessage 用户消息内容
     * @param messageType 消息类型 */
    public void recordUserMessage(TraceContext traceContext, String userMessage, String messageType) {
        if (!traceContext.isTraceEnabled()) {
            return;
        }

        AgentExecutionDetailEntity detail = AgentExecutionDetailEntity.createUserMessageStep(traceContext.getTraceId(),
                traceContext.nextSequence(), userMessage, messageType);

        detailRepository.insert(detail);
    }

    /** 记录用户消息（带时间戳）
     * 
     * @param traceContext 追踪上下文
     * @param userMessage 用户消息内容
     * @param messageType 消息类型
     * @param eventTime 事件发生时间 */
    public void recordUserMessage(TraceContext traceContext, String userMessage, String messageType,
            LocalDateTime eventTime) {
        if (!traceContext.isTraceEnabled()) {
            return;
        }

        AgentExecutionDetailEntity detail = AgentExecutionDetailEntity.createUserMessageStep(traceContext.getTraceId(),
                traceContext.nextSequence(), userMessage, messageType, eventTime);

        detailRepository.insert(detail);
    }

    /** 记录带Token信息的用户消息
     * 
     * @param traceContext 追踪上下文
     * @param userMessage 用户消息内容
     * @param messageType 消息类型
     * @param messageTokens 消息Token数 */
    public void recordUserMessageWithTokens(TraceContext traceContext, String userMessage, String messageType,
            Integer messageTokens) {
        if (!traceContext.isTraceEnabled()) {
            return;
        }

        AgentExecutionDetailEntity detail = AgentExecutionDetailEntity.createUserMessageStepWithTokens(
                traceContext.getTraceId(), traceContext.nextSequence(), userMessage, messageType, messageTokens);

        detailRepository.insert(detail);
    }

    /** 记录带Token信息的用户消息（带时间戳）
     * 
     * @param traceContext 追踪上下文
     * @param userMessage 用户消息内容
     * @param messageType 消息类型
     * @param messageTokens 消息Token数
     * @param eventTime 事件发生时间 */
    public void recordUserMessageWithTokens(TraceContext traceContext, String userMessage, String messageType,
            Integer messageTokens, LocalDateTime eventTime) {
        if (!traceContext.isTraceEnabled()) {
            return;
        }

        AgentExecutionDetailEntity detail = AgentExecutionDetailEntity.createUserMessageStepWithTokens(
                traceContext.getTraceId(), traceContext.nextSequence(), userMessage, messageType, messageTokens,
                eventTime);

        detailRepository.insert(detail);
    }

    /** 记录AI响应
     * 
     * @param traceContext 追踪上下文
     * @param aiResponse AI响应内容
     * @param modelCallInfo 模型调用信息 */
    public void recordAiResponse(TraceContext traceContext, String aiResponse, ModelCallInfo modelCallInfo) {
        if (!traceContext.isTraceEnabled()) {
            return;
        }

        AgentExecutionDetailEntity detail = AgentExecutionDetailEntity.createAiResponseStep(traceContext.getTraceId(),
                traceContext.nextSequence(), aiResponse, modelCallInfo.getModelId(), modelCallInfo.getProviderName(),
                modelCallInfo.getOutputTokens(), // AI响应使用输出Token数
                modelCallInfo.getCallTime(), modelCallInfo.getCost());

        // 设置降级信息
        if (Boolean.TRUE.equals(modelCallInfo.getFallbackUsed())) {
            detail.setFallbackInfo(modelCallInfo.getFallbackReason(), modelCallInfo.getOriginalModel(),
                    modelCallInfo.getModelId());
        }

        // 设置错误信息
        if (Boolean.FALSE.equals(modelCallInfo.getSuccess())) {
            detail.markStepFailed(modelCallInfo.getErrorMessage());
        }

        detailRepository.insert(detail);

        // 更新汇总统计
        updateSummaryTokens(traceContext.getTraceId(), modelCallInfo.getInputTokens(), modelCallInfo.getOutputTokens());
        if (modelCallInfo.getCost() != null) {
            updateSummaryCost(traceContext.getTraceId(), modelCallInfo.getCost());
        }
    }

    /** 记录AI响应（带时间戳）
     * 
     * @param traceContext 追踪上下文
     * @param aiResponse AI响应内容
     * @param modelCallInfo 模型调用信息
     * @param eventTime 事件发生时间（AI开始响应的时间） */
    public void recordAiResponse(TraceContext traceContext, String aiResponse, ModelCallInfo modelCallInfo,
            LocalDateTime eventTime) {
        if (!traceContext.isTraceEnabled()) {
            return;
        }

        AgentExecutionDetailEntity detail = AgentExecutionDetailEntity.createAiResponseStep(traceContext.getTraceId(),
                traceContext.nextSequence(), aiResponse, modelCallInfo.getModelId(), modelCallInfo.getProviderName(),
                modelCallInfo.getOutputTokens(), // AI响应使用输出Token数
                modelCallInfo.getCallTime(), modelCallInfo.getCost(), eventTime);

        // 设置降级信息
        if (Boolean.TRUE.equals(modelCallInfo.getFallbackUsed())) {
            detail.setFallbackInfo(modelCallInfo.getFallbackReason(), modelCallInfo.getOriginalModel(),
                    modelCallInfo.getModelId());
        }

        // 设置错误信息
        if (Boolean.FALSE.equals(modelCallInfo.getSuccess())) {
            detail.markStepFailed(modelCallInfo.getErrorMessage());
        }

        detailRepository.insert(detail);

        // 更新汇总统计
        updateSummaryTokens(traceContext.getTraceId(), modelCallInfo.getInputTokens(), modelCallInfo.getOutputTokens());
        if (modelCallInfo.getCost() != null) {
            updateSummaryCost(traceContext.getTraceId(), modelCallInfo.getCost());
        }
    }

    /** 记录工具调用
     * 
     * @param traceContext 追踪上下文
     * @param toolCallInfo 工具调用信息 */
    public void recordToolCall(TraceContext traceContext, ToolCallInfo toolCallInfo) {
        if (!traceContext.isTraceEnabled()) {
            return;
        }

        AgentExecutionDetailEntity detail = AgentExecutionDetailEntity.createToolCallStep(traceContext.getTraceId(),
                traceContext.nextSequence(), toolCallInfo.getToolName(), toolCallInfo.getRequestArgs(),
                toolCallInfo.getResponseData(), toolCallInfo.getExecutionTime(), toolCallInfo.getSuccess());

        // 设置错误信息
        if (Boolean.FALSE.equals(toolCallInfo.getSuccess())) {
            detail.markStepFailed(toolCallInfo.getErrorMessage());
        }

        detailRepository.insert(detail);

        // 更新汇总统计
        updateSummaryToolExecution(traceContext.getTraceId(), toolCallInfo.getExecutionTime());
    }

    /** 记录工具调用（带时间戳）
     * 
     * @param traceContext 追踪上下文
     * @param toolCallInfo 工具调用信息
     * @param eventTime 事件发生时间（工具开始执行的时间） */
    public void recordToolCall(TraceContext traceContext, ToolCallInfo toolCallInfo, LocalDateTime eventTime) {
        if (!traceContext.isTraceEnabled()) {
            return;
        }

        AgentExecutionDetailEntity detail = AgentExecutionDetailEntity.createToolCallStep(traceContext.getTraceId(),
                traceContext.nextSequence(), toolCallInfo.getToolName(), toolCallInfo.getRequestArgs(),
                toolCallInfo.getResponseData(), toolCallInfo.getExecutionTime(), toolCallInfo.getSuccess(), eventTime);

        // 设置错误信息
        if (Boolean.FALSE.equals(toolCallInfo.getSuccess())) {
            detail.markStepFailed(toolCallInfo.getErrorMessage());
        }

        detailRepository.insert(detail);

        // 更新汇总统计
        updateSummaryToolExecution(traceContext.getTraceId(), toolCallInfo.getExecutionTime());
    }

    /** 完成追踪记录
     * 
     * @param traceContext 追踪上下文
     * @param success 是否成功
     * @param errorPhase 错误阶段
     * @param errorMessage 错误信息 */
    public void completeTrace(TraceContext traceContext, boolean success, ExecutionPhase errorPhase,
            String errorMessage) {
        if (!traceContext.isTraceEnabled()) {
            return;
        }

        LambdaQueryWrapper<AgentExecutionSummaryEntity> wrapper = Wrappers.<AgentExecutionSummaryEntity>lambdaQuery()
                .eq(AgentExecutionSummaryEntity::getTraceId, traceContext.getTraceId());
        AgentExecutionSummaryEntity summary = summaryRepository.selectOne(wrapper);

        if (summary == null) {
            throw new BusinessException("追踪记录不存在: " + traceContext.getTraceId());
        }

        String errorPhaseCode = errorPhase != null ? errorPhase.getCode() : null;
        summary.markCompleted(success, errorPhaseCode, errorMessage);
        summaryRepository.updateById(summary);
    }

    /** 根据追踪ID获取完整的执行信息
     * 
     * @param traceId 追踪ID
     * @param userId 用户ID
     * @return 执行汇总 */
    public AgentExecutionSummaryEntity getExecutionSummary(String traceId, String userId) {
        LambdaQueryWrapper<AgentExecutionSummaryEntity> wrapper = Wrappers.<AgentExecutionSummaryEntity>lambdaQuery()
                .eq(AgentExecutionSummaryEntity::getTraceId, traceId);
        AgentExecutionSummaryEntity summary = summaryRepository.selectOne(wrapper);

        if (summary == null) {
            throw new BusinessException("追踪记录不存在");
        }

        // 检查用户权限
        if (!summary.getUserId().equals(userId)) {
            throw new BusinessException("无权限访问此追踪记录");
        }

        return summary;
    }

    /** 获取执行详情列表
     * 
     * @param traceId 追踪ID
     * @param userId 用户ID
     * @return 执行详情列表 */
    public List<AgentExecutionDetailEntity> getExecutionDetails(String traceId, String userId) {
        // 先检查权限
        getExecutionSummary(traceId, userId);

        LambdaQueryWrapper<AgentExecutionDetailEntity> wrapper = Wrappers.<AgentExecutionDetailEntity>lambdaQuery()
                .eq(AgentExecutionDetailEntity::getTraceId, traceId)
                .orderByAsc(AgentExecutionDetailEntity::getSequenceNo);
        return detailRepository.selectList(wrapper);
    }

    /** 分页查询用户的执行历史
     * 
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 执行历史分页数据 */
    public Page<AgentExecutionSummaryEntity> getUserExecutionHistory(String userId, int page, int pageSize) {
        Page<AgentExecutionSummaryEntity> pageObject = new Page<>(page, pageSize);
        LambdaQueryWrapper<AgentExecutionSummaryEntity> wrapper = Wrappers.<AgentExecutionSummaryEntity>lambdaQuery()
                .eq(AgentExecutionSummaryEntity::getUserId, userId)
                .orderByDesc(AgentExecutionSummaryEntity::getExecutionStartTime);
        return summaryRepository.selectPage(pageObject, wrapper);
    }

    /** 查询会话的执行历史
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 执行历史列表 */
    public List<AgentExecutionSummaryEntity> getSessionExecutionHistory(String sessionId, String userId) {
        LambdaQueryWrapper<AgentExecutionSummaryEntity> wrapper = Wrappers.<AgentExecutionSummaryEntity>lambdaQuery()
                .eq(AgentExecutionSummaryEntity::getSessionId, sessionId)
                .eq(AgentExecutionSummaryEntity::getUserId, userId)
                .orderByDesc(AgentExecutionSummaryEntity::getExecutionStartTime);
        return summaryRepository.selectList(wrapper);
    }

    /** 查询用户在指定时间范围内的执行记录
     * 
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 执行记录列表 */
    public List<AgentExecutionSummaryEntity> getUserExecutionsByTimeRange(String userId, LocalDateTime startTime,
            LocalDateTime endTime) {
        LambdaQueryWrapper<AgentExecutionSummaryEntity> wrapper = Wrappers.<AgentExecutionSummaryEntity>lambdaQuery()
                .eq(AgentExecutionSummaryEntity::getUserId, userId)
                .ge(startTime != null, AgentExecutionSummaryEntity::getExecutionStartTime, startTime)
                .le(endTime != null, AgentExecutionSummaryEntity::getExecutionStartTime, endTime)
                .orderByDesc(AgentExecutionSummaryEntity::getExecutionStartTime);
        return summaryRepository.selectList(wrapper);
    }

    /** 查询用户的失败执行记录
     * 
     * @param userId 用户ID
     * @return 失败的执行记录列表 */
    public List<AgentExecutionSummaryEntity> getUserFailedExecutions(String userId) {
        LambdaQueryWrapper<AgentExecutionSummaryEntity> wrapper = Wrappers.<AgentExecutionSummaryEntity>lambdaQuery()
                .eq(AgentExecutionSummaryEntity::getUserId, userId)
                .eq(AgentExecutionSummaryEntity::getExecutionSuccess, false)
                .orderByDesc(AgentExecutionSummaryEntity::getExecutionStartTime);
        return summaryRepository.selectList(wrapper);
    }

    /** 根据追踪ID和消息类型查询执行详情
     * 
     * @param traceId 追踪ID
     * @param messageType 消息类型
     * @return 执行详情列表 */
    public List<AgentExecutionDetailEntity> getExecutionDetailsByMessageType(String traceId, String messageType) {
        LambdaQueryWrapper<AgentExecutionDetailEntity> wrapper = Wrappers.<AgentExecutionDetailEntity>lambdaQuery()
                .eq(AgentExecutionDetailEntity::getTraceId, traceId)
                .eq(AgentExecutionDetailEntity::getMessageType, messageType)
                .orderByAsc(AgentExecutionDetailEntity::getSequenceNo);
        return detailRepository.selectList(wrapper);
    }

    /** 查询追踪中的工具调用记录
     * 
     * @param traceId 追踪ID
     * @return 工具调用记录列表 */
    public List<AgentExecutionDetailEntity> getToolCallsByTraceId(String traceId) {
        return getExecutionDetailsByMessageType(traceId, "TOOL_CALL");
    }

    /** 查询追踪中的模型调用记录
     * 
     * @param traceId 追踪ID
     * @return 模型调用记录列表 */
    public List<AgentExecutionDetailEntity> getModelCallsByTraceId(String traceId) {
        return getExecutionDetailsByMessageType(traceId, "AI_RESPONSE");
    }

    /** 查询追踪中使用降级的记录
     * 
     * @param traceId 追踪ID
     * @return 使用降级的记录列表 */
    public List<AgentExecutionDetailEntity> getFallbackCallsByTraceId(String traceId) {
        LambdaQueryWrapper<AgentExecutionDetailEntity> wrapper = Wrappers.<AgentExecutionDetailEntity>lambdaQuery()
                .eq(AgentExecutionDetailEntity::getTraceId, traceId)
                .eq(AgentExecutionDetailEntity::getIsFallbackUsed, true)
                .orderByAsc(AgentExecutionDetailEntity::getSequenceNo);
        return detailRepository.selectList(wrapper);
    }

    /** 获取用户的执行统计信息
     * 
     * @param userId 用户ID
     * @return 执行统计信息 */
    public ExecutionStatistics getUserExecutionStatistics(String userId) {
        // 统计总执行次数
        LambdaQueryWrapper<AgentExecutionSummaryEntity> totalWrapper = Wrappers
                .<AgentExecutionSummaryEntity>lambdaQuery().eq(AgentExecutionSummaryEntity::getUserId, userId);
        int totalExecutions = Math.toIntExact(summaryRepository.selectCount(totalWrapper));

        // 统计成功执行次数
        LambdaQueryWrapper<AgentExecutionSummaryEntity> successWrapper = Wrappers
                .<AgentExecutionSummaryEntity>lambdaQuery().eq(AgentExecutionSummaryEntity::getUserId, userId)
                .eq(AgentExecutionSummaryEntity::getExecutionSuccess, true);
        int successfulExecutions = Math.toIntExact(summaryRepository.selectCount(successWrapper));

        // 统计Token使用量
        List<AgentExecutionSummaryEntity> executions = summaryRepository.selectList(totalWrapper);
        long totalTokens = executions.stream().mapToLong(e -> e.getTotalTokens() != null ? e.getTotalTokens() : 0L)
                .sum();

        return new ExecutionStatistics(totalExecutions, successfulExecutions, totalTokens);
    }

    /** 获取用户按Agent分组的执行统计数据
     * 
     * @param userId 用户ID
     * @return Agent统计数据列表 */
    public List<AgentStatistics> getUserAgentStatistics(String userId) {
        LambdaQueryWrapper<AgentExecutionSummaryEntity> wrapper = Wrappers.<AgentExecutionSummaryEntity>lambdaQuery()
                .eq(AgentExecutionSummaryEntity::getUserId, userId)
                .orderByDesc(AgentExecutionSummaryEntity::getExecutionStartTime);

        List<AgentExecutionSummaryEntity> executions = summaryRepository.selectList(wrapper);

        // 按agentId分组统计
        return executions.stream()
                .collect(java.util.stream.Collectors.groupingBy(AgentExecutionSummaryEntity::getAgentId)).entrySet()
                .stream().map(entry -> {
                    String agentId = entry.getKey();
                    List<AgentExecutionSummaryEntity> agentExecutions = entry.getValue();

                    // 计算统计信息
                    int totalExecutions = agentExecutions.size();
                    int successfulExecutions = (int) agentExecutions.stream()
                            .filter(e -> Boolean.TRUE.equals(e.getExecutionSuccess())).count();
                    int failedExecutions = totalExecutions - successfulExecutions;
                    double successRate = totalExecutions > 0 ? (double) successfulExecutions / totalExecutions : 0.0;

                    // Token统计
                    int totalTokens = agentExecutions.stream()
                            .mapToInt(e -> e.getTotalTokens() != null ? e.getTotalTokens() : 0).sum();
                    int totalInputTokens = agentExecutions.stream()
                            .mapToInt(e -> e.getTotalInputTokens() != null ? e.getTotalInputTokens() : 0).sum();
                    int totalOutputTokens = agentExecutions.stream()
                            .mapToInt(e -> e.getTotalOutputTokens() != null ? e.getTotalOutputTokens() : 0).sum();

                    // 工具调用统计
                    int totalToolCalls = agentExecutions.stream()
                            .mapToInt(e -> e.getToolCallCount() != null ? e.getToolCallCount() : 0).sum();

                    // 成本统计
                    BigDecimal totalCost = agentExecutions.stream()
                            .map(e -> e.getTotalCost() != null ? e.getTotalCost() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // 会话数统计（去重）
                    int totalSessions = (int) agentExecutions.stream().map(AgentExecutionSummaryEntity::getSessionId)
                            .distinct().count();

                    // 最后执行时间和状态
                    LocalDateTime lastExecutionTime = agentExecutions.stream()
                            .map(AgentExecutionSummaryEntity::getExecutionStartTime).max(LocalDateTime::compareTo)
                            .orElse(null);

                    Boolean lastExecutionSuccess = agentExecutions.stream()
                            .filter(e -> e.getExecutionStartTime().equals(lastExecutionTime)).findFirst()
                            .map(AgentExecutionSummaryEntity::getExecutionSuccess).orElse(null);

                    return new AgentStatistics(agentId, totalExecutions, successfulExecutions, failedExecutions,
                            successRate, totalTokens, totalInputTokens, totalOutputTokens, totalToolCalls, totalCost,
                            totalSessions, lastExecutionTime, lastExecutionSuccess);
                }).sorted((a, b) -> b.getLastExecutionTime().compareTo(a.getLastExecutionTime()))
                .collect(java.util.stream.Collectors.toList());
    }

    /** 获取指定Agent下按Session分组的执行统计数据
     * 
     * @param agentId Agent ID
     * @param userId 用户ID
     * @return Session统计数据列表 */
    public List<SessionStatistics> getAgentSessionStatistics(String agentId, String userId) {
        LambdaQueryWrapper<AgentExecutionSummaryEntity> wrapper = Wrappers.<AgentExecutionSummaryEntity>lambdaQuery()
                .eq(AgentExecutionSummaryEntity::getAgentId, agentId).eq(AgentExecutionSummaryEntity::getUserId, userId)
                .orderByDesc(AgentExecutionSummaryEntity::getExecutionStartTime);

        List<AgentExecutionSummaryEntity> executions = summaryRepository.selectList(wrapper);

        // 按sessionId分组统计
        return executions.stream()
                .collect(java.util.stream.Collectors.groupingBy(AgentExecutionSummaryEntity::getSessionId)).entrySet()
                .stream().map(entry -> {
                    String sessionId = entry.getKey();
                    List<AgentExecutionSummaryEntity> sessionExecutions = entry.getValue();

                    // 计算统计信息
                    int totalExecutions = sessionExecutions.size();
                    int successfulExecutions = (int) sessionExecutions.stream()
                            .filter(e -> Boolean.TRUE.equals(e.getExecutionSuccess())).count();
                    int failedExecutions = totalExecutions - successfulExecutions;
                    double successRate = totalExecutions > 0 ? (double) successfulExecutions / totalExecutions : 0.0;

                    // Token统计
                    int totalTokens = sessionExecutions.stream()
                            .mapToInt(e -> e.getTotalTokens() != null ? e.getTotalTokens() : 0).sum();
                    int totalInputTokens = sessionExecutions.stream()
                            .mapToInt(e -> e.getTotalInputTokens() != null ? e.getTotalInputTokens() : 0).sum();
                    int totalOutputTokens = sessionExecutions.stream()
                            .mapToInt(e -> e.getTotalOutputTokens() != null ? e.getTotalOutputTokens() : 0).sum();

                    // 工具调用统计
                    int totalToolCalls = sessionExecutions.stream()
                            .mapToInt(e -> e.getToolCallCount() != null ? e.getToolCallCount() : 0).sum();

                    // 执行时间统计
                    int totalExecutionTime = sessionExecutions.stream()
                            .mapToInt(e -> e.getTotalExecutionTime() != null ? e.getTotalExecutionTime() : 0).sum();

                    // 成本统计
                    BigDecimal totalCost = sessionExecutions.stream()
                            .map(e -> e.getTotalCost() != null ? e.getTotalCost() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // 最后执行时间和状态
                    LocalDateTime lastExecutionTime = sessionExecutions.stream()
                            .map(AgentExecutionSummaryEntity::getExecutionStartTime).max(LocalDateTime::compareTo)
                            .orElse(null);

                    Boolean lastExecutionSuccess = sessionExecutions.stream()
                            .filter(e -> e.getExecutionStartTime().equals(lastExecutionTime)).findFirst()
                            .map(AgentExecutionSummaryEntity::getExecutionSuccess).orElse(null);

                    return new SessionStatistics(sessionId, agentId, totalExecutions, successfulExecutions,
                            failedExecutions, successRate, totalTokens, totalInputTokens, totalOutputTokens,
                            totalToolCalls, totalExecutionTime, totalCost, lastExecutionTime, lastExecutionSuccess);
                }).sorted((a, b) -> b.getLastExecutionTime().compareTo(a.getLastExecutionTime()))
                .collect(java.util.stream.Collectors.toList());
    }

    /** 更新汇总的Token统计 */
    private void updateSummaryTokens(String traceId, Integer inputTokens, Integer outputTokens) {
        LambdaQueryWrapper<AgentExecutionSummaryEntity> wrapper = Wrappers.<AgentExecutionSummaryEntity>lambdaQuery()
                .eq(AgentExecutionSummaryEntity::getTraceId, traceId);
        AgentExecutionSummaryEntity summary = summaryRepository.selectOne(wrapper);

        if (summary != null) {
            summary.addTokens(inputTokens, outputTokens);
            summaryRepository.updateById(summary);
        }
    }

    /** 更新汇总的成本统计 */
    private void updateSummaryCost(String traceId, BigDecimal cost) {
        LambdaQueryWrapper<AgentExecutionSummaryEntity> wrapper = Wrappers.<AgentExecutionSummaryEntity>lambdaQuery()
                .eq(AgentExecutionSummaryEntity::getTraceId, traceId);
        AgentExecutionSummaryEntity summary = summaryRepository.selectOne(wrapper);

        if (summary != null) {
            summary.addCost(cost);
            summaryRepository.updateById(summary);
        }
    }

    /** 更新汇总的工具执行统计 */
    private void updateSummaryToolExecution(String traceId, Integer executionTime) {
        LambdaQueryWrapper<AgentExecutionSummaryEntity> wrapper = Wrappers.<AgentExecutionSummaryEntity>lambdaQuery()
                .eq(AgentExecutionSummaryEntity::getTraceId, traceId);
        AgentExecutionSummaryEntity summary = summaryRepository.selectOne(wrapper);

        if (summary != null) {
            summary.addToolExecution(executionTime);
            summaryRepository.updateById(summary);
        }
    }

    /** 生成追踪ID */
    private String generateTraceId() {
        return "trace_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** 执行统计信息 */
    public static class ExecutionStatistics {
        private final int totalExecutions;
        private final int successfulExecutions;
        private final long totalTokens;

        public ExecutionStatistics(int totalExecutions, int successfulExecutions, long totalTokens) {
            this.totalExecutions = totalExecutions;
            this.successfulExecutions = successfulExecutions;
            this.totalTokens = totalTokens;
        }

        public int getTotalExecutions() {
            return totalExecutions;
        }

        public int getSuccessfulExecutions() {
            return successfulExecutions;
        }

        public int getFailedExecutions() {
            return totalExecutions - successfulExecutions;
        }

        public double getSuccessRate() {
            return totalExecutions > 0 ? (double) successfulExecutions / totalExecutions : 0.0;
        }

        public long getTotalTokens() {
            return totalTokens;
        }
    }

    /** Agent执行统计信息 */
    public static class AgentStatistics {
        private final String agentId;
        private final int totalExecutions;
        private final int successfulExecutions;
        private final int failedExecutions;
        private final double successRate;
        private final int totalTokens;
        private final int totalInputTokens;
        private final int totalOutputTokens;
        private final int totalToolCalls;
        private final BigDecimal totalCost;
        private final int totalSessions;
        private final LocalDateTime lastExecutionTime;
        private final Boolean lastExecutionSuccess;

        public AgentStatistics(String agentId, int totalExecutions, int successfulExecutions, int failedExecutions,
                double successRate, int totalTokens, int totalInputTokens, int totalOutputTokens, int totalToolCalls,
                BigDecimal totalCost, int totalSessions, LocalDateTime lastExecutionTime,
                Boolean lastExecutionSuccess) {
            this.agentId = agentId;
            this.totalExecutions = totalExecutions;
            this.successfulExecutions = successfulExecutions;
            this.failedExecutions = failedExecutions;
            this.successRate = successRate;
            this.totalTokens = totalTokens;
            this.totalInputTokens = totalInputTokens;
            this.totalOutputTokens = totalOutputTokens;
            this.totalToolCalls = totalToolCalls;
            this.totalCost = totalCost;
            this.totalSessions = totalSessions;
            this.lastExecutionTime = lastExecutionTime;
            this.lastExecutionSuccess = lastExecutionSuccess;
        }

        // Getter方法
        public String getAgentId() {
            return agentId;
        }
        public int getTotalExecutions() {
            return totalExecutions;
        }
        public int getSuccessfulExecutions() {
            return successfulExecutions;
        }
        public int getFailedExecutions() {
            return failedExecutions;
        }
        public double getSuccessRate() {
            return successRate;
        }
        public int getTotalTokens() {
            return totalTokens;
        }
        public int getTotalInputTokens() {
            return totalInputTokens;
        }
        public int getTotalOutputTokens() {
            return totalOutputTokens;
        }
        public int getTotalToolCalls() {
            return totalToolCalls;
        }
        public BigDecimal getTotalCost() {
            return totalCost;
        }
        public int getTotalSessions() {
            return totalSessions;
        }
        public LocalDateTime getLastExecutionTime() {
            return lastExecutionTime;
        }
        public Boolean getLastExecutionSuccess() {
            return lastExecutionSuccess;
        }
    }

    /** 会话执行统计信息 */
    public static class SessionStatistics {
        private final String sessionId;
        private final String agentId;
        private final int totalExecutions;
        private final int successfulExecutions;
        private final int failedExecutions;
        private final double successRate;
        private final int totalTokens;
        private final int totalInputTokens;
        private final int totalOutputTokens;
        private final int totalToolCalls;
        private final int totalExecutionTime;
        private final BigDecimal totalCost;
        private final LocalDateTime lastExecutionTime;
        private final Boolean lastExecutionSuccess;

        public SessionStatistics(String sessionId, String agentId, int totalExecutions, int successfulExecutions,
                int failedExecutions, double successRate, int totalTokens, int totalInputTokens, int totalOutputTokens,
                int totalToolCalls, int totalExecutionTime, BigDecimal totalCost, LocalDateTime lastExecutionTime,
                Boolean lastExecutionSuccess) {
            this.sessionId = sessionId;
            this.agentId = agentId;
            this.totalExecutions = totalExecutions;
            this.successfulExecutions = successfulExecutions;
            this.failedExecutions = failedExecutions;
            this.successRate = successRate;
            this.totalTokens = totalTokens;
            this.totalInputTokens = totalInputTokens;
            this.totalOutputTokens = totalOutputTokens;
            this.totalToolCalls = totalToolCalls;
            this.totalExecutionTime = totalExecutionTime;
            this.totalCost = totalCost;
            this.lastExecutionTime = lastExecutionTime;
            this.lastExecutionSuccess = lastExecutionSuccess;
        }

        // Getter方法
        public String getSessionId() {
            return sessionId;
        }
        public String getAgentId() {
            return agentId;
        }
        public int getTotalExecutions() {
            return totalExecutions;
        }
        public int getSuccessfulExecutions() {
            return successfulExecutions;
        }
        public int getFailedExecutions() {
            return failedExecutions;
        }
        public double getSuccessRate() {
            return successRate;
        }
        public int getTotalTokens() {
            return totalTokens;
        }
        public int getTotalInputTokens() {
            return totalInputTokens;
        }
        public int getTotalOutputTokens() {
            return totalOutputTokens;
        }
        public int getTotalToolCalls() {
            return totalToolCalls;
        }
        public int getTotalExecutionTime() {
            return totalExecutionTime;
        }
        public BigDecimal getTotalCost() {
            return totalCost;
        }
        public LocalDateTime getLastExecutionTime() {
            return lastExecutionTime;
        }
        public Boolean getLastExecutionSuccess() {
            return lastExecutionSuccess;
        }
    }
}