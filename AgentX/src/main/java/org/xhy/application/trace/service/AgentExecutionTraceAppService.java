package org.xhy.application.trace.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.xhy.application.trace.assembler.AgentExecutionTraceAssembler;
import org.xhy.application.trace.dto.*;
import org.xhy.domain.trace.model.AgentExecutionDetailEntity;
import org.xhy.domain.trace.model.AgentExecutionSummaryEntity;
import org.xhy.domain.trace.service.AgentExecutionTraceDomainService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent执行链路追踪应用服务
 * 协调追踪数据的查询和展示逻辑
 */
@Service
public class AgentExecutionTraceAppService {

    private final AgentExecutionTraceDomainService traceDomainService;

    public AgentExecutionTraceAppService(AgentExecutionTraceDomainService traceDomainService) {
        this.traceDomainService = traceDomainService;
    }

    /**
     * 获取完整的执行链路信息
     * 
     * @param traceId 追踪ID
     * @param userId 用户ID
     * @return 完整的执行链路DTO
     */
    public ExecutionTraceDTO getExecutionTrace(String traceId, Long userId) {
        // 获取汇总信息
        AgentExecutionSummaryEntity summary = traceDomainService.getExecutionSummary(traceId, userId);
        
        // 获取详细信息
        List<AgentExecutionDetailEntity> details = traceDomainService.getExecutionDetails(traceId, userId);
        
        // 转换为DTO
        return AgentExecutionTraceAssembler.toExecutionTraceDTO(summary, details);
    }

    /**
     * 分页查询用户的执行历史
     * 
     * @param userId 用户ID
     * @param request 查询请求参数
     * @return 执行历史分页数据
     */
    public Page<AgentExecutionSummaryDTO> getUserExecutionHistory(Long userId, QueryExecutionHistoryRequest request) {
        // 构建查询条件并执行分页查询
        Page<AgentExecutionSummaryEntity> entityPage = traceDomainService.getUserExecutionHistory(
                userId, 
                request.getPage() != null ? request.getPage() : 1, 
                request.getPageSize() != null ? request.getPageSize() : 15
        );
        
        // 转换为DTO分页结果
        Page<AgentExecutionSummaryDTO> dtoPage = new Page<>(
                entityPage.getCurrent(),
                entityPage.getSize(),
                entityPage.getTotal()
        );
        
        List<AgentExecutionSummaryDTO> dtoList = AgentExecutionTraceAssembler.toSummaryDTOs(entityPage.getRecords());
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }

    /**
     * 查询会话的执行历史
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 执行历史列表
     */
    public List<AgentExecutionSummaryDTO> getSessionExecutionHistory(String sessionId, Long userId) {
        List<AgentExecutionSummaryEntity> entities = traceDomainService.getSessionExecutionHistory(sessionId, userId);
        return AgentExecutionTraceAssembler.toSummaryDTOs(entities);
    }

    /**
     * 查询用户在指定时间范围内的执行记录
     * 
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 执行记录列表
     */
    public List<AgentExecutionSummaryDTO> getUserExecutionsByTimeRange(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        List<AgentExecutionSummaryEntity> entities = traceDomainService.getUserExecutionsByTimeRange(userId, startTime, endTime);
        return AgentExecutionTraceAssembler.toSummaryDTOs(entities);
    }

    /**
     * 查询用户的失败执行记录
     * 
     * @param userId 用户ID
     * @return 失败的执行记录列表
     */
    public List<AgentExecutionSummaryDTO> getUserFailedExecutions(Long userId) {
        List<AgentExecutionSummaryEntity> entities = traceDomainService.getUserFailedExecutions(userId);
        return AgentExecutionTraceAssembler.toSummaryDTOs(entities);
    }

    /**
     * 获取用户的执行统计信息
     * 
     * @param userId 用户ID
     * @return 执行统计信息
     */
    public ExecutionStatisticsDTO getUserExecutionStatistics(Long userId) {
        AgentExecutionTraceDomainService.ExecutionStatistics statistics = 
                traceDomainService.getUserExecutionStatistics(userId);
        return AgentExecutionTraceAssembler.toStatisticsDTO(statistics);
    }

    /**
     * 获取追踪中的工具调用记录
     * 
     * @param traceId 追踪ID
     * @param userId 用户ID
     * @return 工具调用记录列表
     */
    public List<AgentExecutionDetailDTO> getToolCallsByTraceId(String traceId, Long userId) {
        // 先检查权限
        traceDomainService.getExecutionSummary(traceId, userId);
        
        List<AgentExecutionDetailEntity> entities = traceDomainService.getToolCallsByTraceId(traceId);
        return AgentExecutionTraceAssembler.toDetailDTOs(entities);
    }

    /**
     * 获取追踪中的模型调用记录
     * 
     * @param traceId 追踪ID
     * @param userId 用户ID
     * @return 模型调用记录列表
     */
    public List<AgentExecutionDetailDTO> getModelCallsByTraceId(String traceId, Long userId) {
        // 先检查权限
        traceDomainService.getExecutionSummary(traceId, userId);
        
        List<AgentExecutionDetailEntity> entities = traceDomainService.getModelCallsByTraceId(traceId);
        return AgentExecutionTraceAssembler.toDetailDTOs(entities);
    }

    /**
     * 获取追踪中使用降级的记录
     * 
     * @param traceId 追踪ID
     * @param userId 用户ID
     * @return 使用降级的记录列表
     */
    public List<AgentExecutionDetailDTO> getFallbackCallsByTraceId(String traceId, Long userId) {
        // 先检查权限
        traceDomainService.getExecutionSummary(traceId, userId);
        
        List<AgentExecutionDetailEntity> entities = traceDomainService.getFallbackCallsByTraceId(traceId);
        return AgentExecutionTraceAssembler.toDetailDTOs(entities);
    }
}