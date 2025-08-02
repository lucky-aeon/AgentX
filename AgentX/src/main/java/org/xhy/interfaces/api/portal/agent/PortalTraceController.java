package org.xhy.interfaces.api.portal.agent;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.trace.dto.*;
import org.xhy.application.trace.service.AgentExecutionTraceAppService;
import org.xhy.domain.trace.service.AgentExecutionTraceDomainService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;

import java.util.List;

/** 用户级别Agent执行链路追踪查询接口 */
@RestController
@RequestMapping("/traces")
public class PortalTraceController {

    private final AgentExecutionTraceAppService traceAppService;
    private final AgentExecutionTraceDomainService traceDomainService;

    public PortalTraceController(AgentExecutionTraceAppService traceAppService,
            AgentExecutionTraceDomainService traceDomainService) {
        this.traceAppService = traceAppService;
        this.traceDomainService = traceDomainService;
    }

    /** 分页查询用户执行历史
     * 
     * @param request 查询条件
     * @return 执行历史分页数据 */
    @GetMapping("/history")
    public Result<Page<AgentExecutionSummaryDTO>> getExecutionHistory(QueryExecutionHistoryRequest request) {
        String userId = UserContext.getCurrentUserId();
        Page<AgentExecutionSummaryDTO> result = traceAppService.getExecutionHistory(request, userId);
        return Result.success(result);
    }

    /** 获取单个追踪详情
     * 
     * @param traceId 追踪ID
     * @return 追踪详情 */
    @GetMapping("/{traceId}")
    public Result<TraceDetailResponse> getTraceDetail(@PathVariable String traceId) {
        String userId = UserContext.getCurrentUserId();
        TraceDetailResponse result = traceAppService.getTraceDetail(traceId, userId);
        return Result.success(result);
    }

    /** 获取执行详情列表
     * 
     * @param traceId 追踪ID
     * @return 执行详情列表 */
    @GetMapping("/{traceId}/details")
    public Result<List<AgentExecutionDetailDTO>> getExecutionDetails(@PathVariable String traceId) {
        String userId = UserContext.getCurrentUserId();
        List<AgentExecutionDetailDTO> result = traceAppService.getExecutionDetails(traceId, userId);
        return Result.success(result);
    }

    /** 查询会话执行记录
     * 
     * @param sessionId 会话ID
     * @return 会话执行记录列表 */
    @GetMapping("/sessions/{sessionId}")
    public Result<List<AgentExecutionSummaryDTO>> getSessionExecutionHistory(@PathVariable String sessionId) {
        String userId = UserContext.getCurrentUserId();
        List<AgentExecutionSummaryDTO> result = traceAppService.getSessionExecutionHistoryByUserId(sessionId, userId);
        return Result.success(result);
    }

    /** 获取用户执行统计
     * 
     * @return 执行统计信息 */
    @GetMapping("/statistics")
    public Result<ExecutionStatisticsResponse> getUserExecutionStatistics() {
        String userId = UserContext.getCurrentUserId();

        AgentExecutionTraceDomainService.ExecutionStatistics statistics = traceDomainService
                .getUserExecutionStatistics(userId);

        ExecutionStatisticsResponse response = new ExecutionStatisticsResponse(statistics.getTotalExecutions(),
                statistics.getSuccessfulExecutions(), statistics.getFailedExecutions(), statistics.getSuccessRate(),
                statistics.getTotalTokens());

        return Result.success(response);
    }

    /** 获取用户的Agent执行链路统计信息
     * 
     * @param request 查询条件
     * @return Agent统计信息列表 */
    @GetMapping("/agents")
    public Result<List<AgentTraceStatisticsDTO>> getUserAgentTraceStatistics(AgentTraceListRequest request) {
        String userId = UserContext.getCurrentUserId();
        List<AgentTraceStatisticsDTO> result = traceAppService.getUserAgentTraceStatistics(request, userId);
        return Result.success(result);
    }

    /** 获取指定Agent下的会话执行链路统计信息
     * 
     * @param agentId Agent ID
     * @param request 查询条件
     * @return 会话统计信息列表 */
    @GetMapping("/agents/{agentId}/sessions")
    public Result<List<SessionTraceStatisticsDTO>> getAgentSessionTraceStatistics(
            @PathVariable String agentId, SessionTraceListRequest request) {
        String userId = UserContext.getCurrentUserId();
        List<SessionTraceStatisticsDTO> result = traceAppService.getAgentSessionTraceStatistics(agentId, request, userId);
        return Result.success(result);
    }
}