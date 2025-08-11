package org.xhy.application.trace.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.application.trace.assembler.AgentExecutionTraceAssembler;
import org.xhy.application.trace.dto.*;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.agent.model.AgentVersionEntity;
import org.xhy.domain.agent.service.AgentDomainService;
import org.xhy.domain.conversation.model.SessionEntity;
import org.xhy.domain.conversation.service.SessionDomainService;
import org.xhy.domain.trace.model.AgentExecutionDetailEntity;
import org.xhy.domain.trace.model.AgentExecutionSummaryEntity;
import org.xhy.domain.trace.service.AgentExecutionTraceDomainService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Agent执行链路追踪应用服务 协调追踪数据的查询和展示逻辑 */
@Service
public class AgentExecutionTraceAppService {

    private static final Logger logger = LoggerFactory.getLogger(AgentExecutionTraceAppService.class);

    private final AgentExecutionTraceDomainService traceDomainService;
    private final AgentDomainService agentDomainService;
    private final SessionDomainService sessionDomainService;

    public AgentExecutionTraceAppService(AgentExecutionTraceDomainService traceDomainService,
            AgentDomainService agentDomainService, SessionDomainService sessionDomainService) {
        this.traceDomainService = traceDomainService;
        this.agentDomainService = agentDomainService;
        this.sessionDomainService = sessionDomainService;
    }

    /** 获取完整的执行链路信息
     * 
     * @param traceId 追踪ID
     * @param userId 用户ID
     * @return 完整的执行链路DTO */
    public ExecutionTraceDTO getExecutionTrace(String traceId, String userId) {
        // 获取汇总信息
        AgentExecutionSummaryEntity summary = traceDomainService.getExecutionSummary(traceId, userId);

        // 获取详细信息
        List<AgentExecutionDetailEntity> details = traceDomainService.getExecutionDetails(traceId, userId);

        // 转换为DTO
        return AgentExecutionTraceAssembler.toExecutionTraceDTO(summary, details);
    }

    /** 分页查询用户的执行历史
     * 
     * @param userId 用户ID
     * @param request 查询请求参数
     * @return 执行历史分页数据 */
    public Page<AgentExecutionSummaryDTO> getUserExecutionHistory(String userId, QueryExecutionHistoryRequest request) {
        // 构建查询条件并执行分页查询
        Page<AgentExecutionSummaryEntity> entityPage = traceDomainService.getUserExecutionHistory(userId,
                request.getPage() != null ? request.getPage() : 1,
                request.getPageSize() != null ? request.getPageSize() : 15);

        // 转换为DTO分页结果
        Page<AgentExecutionSummaryDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(),
                entityPage.getTotal());

        List<AgentExecutionSummaryDTO> dtoList = AgentExecutionTraceAssembler.toSummaryDTOs(entityPage.getRecords());
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }

    /** 查询会话的执行历史
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 执行历史列表 */
    public List<AgentExecutionSummaryDTO> getSessionExecutionHistory(String sessionId, String userId) {
        List<AgentExecutionSummaryEntity> entities = traceDomainService.getSessionExecutionHistory(sessionId, userId);
        return AgentExecutionTraceAssembler.toSummaryDTOs(entities);
    }

    /** 查询用户在指定时间范围内的执行记录
     * 
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 执行记录列表 */
    public List<AgentExecutionSummaryDTO> getUserExecutionsByTimeRange(String userId, LocalDateTime startTime,
            LocalDateTime endTime) {
        List<AgentExecutionSummaryEntity> entities = traceDomainService.getUserExecutionsByTimeRange(userId, startTime,
                endTime);
        return AgentExecutionTraceAssembler.toSummaryDTOs(entities);
    }

    /** 查询用户的失败执行记录
     * 
     * @param userId 用户ID
     * @return 失败的执行记录列表 */
    public List<AgentExecutionSummaryDTO> getUserFailedExecutions(String userId) {
        List<AgentExecutionSummaryEntity> entities = traceDomainService.getUserFailedExecutions(userId);
        return AgentExecutionTraceAssembler.toSummaryDTOs(entities);
    }

    /** 获取用户的执行统计信息
     * 
     * @param userId 用户ID
     * @return 执行统计信息 */
    public ExecutionStatisticsDTO getUserExecutionStatistics(String userId) {
        AgentExecutionTraceDomainService.ExecutionStatistics statistics = traceDomainService
                .getUserExecutionStatistics(userId);
        return AgentExecutionTraceAssembler.toStatisticsDTO(statistics);
    }

    /** 获取追踪中的工具调用记录
     * 
     * @param traceId 追踪ID
     * @param userId 用户ID
     * @return 工具调用记录列表 */
    public List<AgentExecutionDetailDTO> getToolCallsByTraceId(String traceId, String userId) {
        // 先检查权限
        traceDomainService.getExecutionSummary(traceId, userId);

        List<AgentExecutionDetailEntity> entities = traceDomainService.getToolCallsBySessionId(traceId);
        return AgentExecutionTraceAssembler.toDetailDTOs(entities);
    }

    /** 获取追踪中的模型调用记录
     * 
     * @param traceId 追踪ID
     * @param userId 用户ID
     * @return 模型调用记录列表 */
    public List<AgentExecutionDetailDTO> getModelCallsByTraceId(String traceId, String userId) {
        // 先检查权限
        traceDomainService.getExecutionSummary(traceId, userId);

        List<AgentExecutionDetailEntity> entities = traceDomainService.getModelCallsBySessionId(traceId);
        return AgentExecutionTraceAssembler.toDetailDTOs(entities);
    }

    /** 获取追踪中使用降级的记录
     * 
     * @param traceId 追踪ID
     * @param userId 用户ID
     * @return 使用降级的记录列表 */
    public List<AgentExecutionDetailDTO> getFallbackCallsByTraceId(String traceId, String userId) {
        // 先检查权限
        traceDomainService.getExecutionSummary(traceId, userId);

        List<AgentExecutionDetailEntity> entities = traceDomainService.getFallbackCallsBySessionId(traceId);
        return AgentExecutionTraceAssembler.toDetailDTOs(entities);
    }

    /** 分页查询执行历史 - 控制器适配方法
     * 
     * @param request 查询请求
     * @param userId 用户ID
     * @return 执行历史分页数据 */
    public Page<AgentExecutionSummaryDTO> getExecutionHistory(QueryExecutionHistoryRequest request, String userId) {
        return getUserExecutionHistory(userId, request);
    }

    /** 获取追踪详情 - 控制器适配方法
     * 
     * @param traceId 追踪ID
     * @param userId 用户ID
     * @return 追踪详情 */
    public TraceDetailResponse getTraceDetail(String traceId, String userId) {
        // 获取汇总信息
        AgentExecutionSummaryEntity summary = traceDomainService.getExecutionSummary(traceId, userId);

        // 获取详细信息
        List<AgentExecutionDetailEntity> details = traceDomainService.getExecutionDetails(traceId, userId);

        // 转换为DTO
        AgentExecutionSummaryDTO summaryDTO = AgentExecutionTraceAssembler.toSummaryDTO(summary);
        List<AgentExecutionDetailDTO> detailDTOs = AgentExecutionTraceAssembler.toDetailDTOs(details);

        return new TraceDetailResponse(summaryDTO, detailDTOs);
    }

    /** 获取执行详情列表 - 控制器适配方法
     * 
     * @param traceId 追踪ID
     * @param userId 用户ID
     * @return 执行详情列表 */
    public List<AgentExecutionDetailDTO> getExecutionDetails(String traceId, String userId) {
        List<AgentExecutionDetailEntity> entities = traceDomainService.getExecutionDetails(traceId, userId);
        return AgentExecutionTraceAssembler.toDetailDTOs(entities);
    }

    /** 查询会话执行历史 - 控制器适配方法
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 会话执行记录列表 */
    public List<AgentExecutionSummaryDTO> getSessionExecutionHistoryByUserId(String sessionId, String userId) {
        List<AgentExecutionSummaryEntity> entities = traceDomainService.getSessionExecutionHistory(sessionId, userId);
        List<AgentExecutionSummaryDTO> dtoList = AgentExecutionTraceAssembler.toSummaryDTOs(entities);

        // 填充Agent名称
        for (AgentExecutionSummaryDTO dto : dtoList) {
            if (dto.getAgentId() != null) {
                String agentName = getAgentName(dto.getAgentId(), userId);
                dto.setAgentName(agentName);
            }
        }

        return dtoList;
    }

    /** 获取用户的Agent执行链路统计信息（含Agent名称）
     * 
     * @param request 查询请求
     * @param userId 用户ID
     * @return Agent统计信息列表 */
    public List<AgentTraceStatisticsDTO> getUserAgentTraceStatistics(AgentTraceListRequest request, String userId) {
        // 获取领域统计数据
        List<AgentExecutionTraceDomainService.AgentStatistics> agentStatistics = traceDomainService
                .getUserAgentStatistics(userId);

        if (agentStatistics.isEmpty()) {
            return List.of();
        }

        // 提取所有agentId，批量获取Agent信息
        List<String> agentIds = agentStatistics.stream()
                .map(AgentExecutionTraceDomainService.AgentStatistics::getAgentId).collect(Collectors.toList());

        // 批量获取Agent名称映射（容错处理）
        Map<String, String> agentNameMap = getAgentNameMap(agentIds, userId);

        // 转换为DTO并填充Agent名称
        return agentStatistics.stream().map(stats -> {
            AgentTraceStatisticsDTO dto = new AgentTraceStatisticsDTO();
            dto.setAgentId(stats.getAgentId());
            dto.setAgentName(agentNameMap.getOrDefault(stats.getAgentId(), "未知助理"));
            dto.setTotalExecutions(stats.getTotalExecutions());
            dto.setSuccessfulExecutions(stats.getSuccessfulExecutions());
            dto.setFailedExecutions(stats.getFailedExecutions());
            dto.setSuccessRate(stats.getSuccessRate());
            dto.setTotalTokens(stats.getTotalTokens());
            dto.setTotalInputTokens(stats.getTotalInputTokens());
            dto.setTotalOutputTokens(stats.getTotalOutputTokens());
            dto.setTotalToolCalls(stats.getTotalToolCalls());
            dto.setTotalCost(stats.getTotalCost());
            dto.setTotalSessions(stats.getTotalSessions());
            dto.setLastExecutionTime(stats.getLastExecutionTime());
            dto.setLastExecutionSuccess(stats.getLastExecutionSuccess());
            return dto;
        }).collect(Collectors.toList());
    }

    /** 获取指定Agent下的会话执行链路统计信息（含会话名称）
     * 
     * @param agentId Agent ID
     * @param request 查询请求
     * @param userId 用户ID
     * @return 会话统计信息列表 */
    public List<SessionTraceStatisticsDTO> getAgentSessionTraceStatistics(String agentId,
            SessionTraceListRequest request, String userId) {
        // 获取领域统计数据
        List<AgentExecutionTraceDomainService.SessionStatistics> sessionStatistics = traceDomainService
                .getAgentSessionStatistics(agentId, userId);

        if (sessionStatistics.isEmpty()) {
            return List.of();
        }

        // 获取Agent名称
        String agentName = getAgentName(agentId, userId);

        // 提取所有sessionId，批量获取会话信息
        List<String> sessionIds = sessionStatistics.stream()
                .map(AgentExecutionTraceDomainService.SessionStatistics::getSessionId).collect(Collectors.toList());

        // 批量获取会话标题映射（容错处理）
        Map<String, SessionEntity> sessionMap = getSessionMap(sessionIds, userId);

        // 转换为DTO并填充会话信息
        return sessionStatistics.stream().map(stats -> {
            SessionTraceStatisticsDTO dto = new SessionTraceStatisticsDTO();
            dto.setSessionId(stats.getSessionId());
            dto.setAgentId(stats.getAgentId());
            dto.setAgentName(agentName);

            // 设置会话标题和创建时间
            SessionEntity session = sessionMap.get(stats.getSessionId());
            if (session != null) {
                dto.setSessionTitle(session.getTitle());
                dto.setSessionCreatedTime(session.getCreatedAt());
            } else {
                dto.setSessionTitle("未知会话");
                dto.setIsArchived(false);
            }

            dto.setTotalExecutions(stats.getTotalExecutions());
            dto.setSuccessfulExecutions(stats.getSuccessfulExecutions());
            dto.setFailedExecutions(stats.getFailedExecutions());
            dto.setSuccessRate(stats.getSuccessRate());
            dto.setTotalTokens(stats.getTotalTokens());
            dto.setTotalInputTokens(stats.getTotalInputTokens());
            dto.setTotalOutputTokens(stats.getTotalOutputTokens());
            dto.setTotalToolCalls(stats.getTotalToolCalls());
            dto.setTotalExecutionTime(stats.getTotalExecutionTime());
            dto.setTotalCost(stats.getTotalCost());
            dto.setLastExecutionTime(stats.getLastExecutionTime());
            dto.setLastExecutionSuccess(stats.getLastExecutionSuccess());
            return dto;
        }).collect(Collectors.toList());
    }

    /** 批量获取Agent名称映射 */
    private Map<String, String> getAgentNameMap(List<String> agentIds, String userId) {
        return agentIds.stream()
                .collect(Collectors.toMap(Function.identity(), agentId -> getAgentName(agentId, userId)));
    }

    /** 获取单个Agent名称（容错处理） */
    private String getAgentName(String agentId, String userId) {
        try {
            // 1. 先尝试通过用户权限获取Agent（用户自己的Agent）
            AgentEntity agent = agentDomainService.getAgent(agentId, userId);
            logger.debug("成功获取用户 {} 自己的Agent {}，名称: {}", userId, agentId, agent.getName());
            return agent != null ? agent.getName() : "未知助理";
        } catch (Exception e) {
            logger.debug("无法通过用户权限获取Agent {}，尝试其他查询方式: {}", agentId, e.getMessage());

            // 2. 尝试查询已发布的版本（按agent_id查询）
            try {
                AgentVersionEntity publishedVersion = agentDomainService.getPublishedAgentVersion(agentId);
                if (publishedVersion != null) {
                    logger.debug("成功获取已发布Agent版本 {}，名称: {}", agentId, publishedVersion.getName());
                    return publishedVersion.getName();
                }
            } catch (Exception ex) {
                logger.debug("按agent_id查询已发布版本失败: {}", ex.getMessage());
            }

            // 3. 如果上述都失败，可能agentId实际上是version_id，尝试直接查询版本记录
            try {
                AgentVersionEntity versionEntity = agentDomainService.getAgentVersionById(agentId);
                if (versionEntity != null) {
                    logger.debug("发现agentId {} 实际是版本ID，获取版本名称: {}", agentId, versionEntity.getName());
                    return versionEntity.getName();
                }
            } catch (Exception ex) {
                logger.debug("按版本ID查询失败: {}", ex.getMessage());
            }

            logger.warn("所有查询方式都失败，Agent ID: {}", agentId);
            return "未知助理";
        }
    }

    /** 批量获取会话映射 */
    private Map<String, SessionEntity> getSessionMap(List<String> sessionIds, String userId) {
        return sessionIds.stream()
                .collect(Collectors.toMap(Function.identity(), sessionId -> getSession(sessionId, userId))).entrySet()
                .stream().filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** 获取单个会话信息（容错处理） */
    private SessionEntity getSession(String sessionId, String userId) {
        try {
            return sessionDomainService.getSession(sessionId, userId);
        } catch (Exception e) {
            // 如果会话不存在或无权限访问，返回null
            return null;
        }
    }
}