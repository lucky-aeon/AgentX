package org.xhy.interfaces.api.portal.agent;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.agent.service.AgentAppService;
import org.xhy.application.agent.dto.AgentDTO;
import org.xhy.application.agent.dto.AgentVersionDTO;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.agent.request.CreateAgentRequest;
import org.xhy.interfaces.dto.agent.request.PublishAgentVersionRequest;
import org.xhy.interfaces.dto.agent.request.SearchAgentsRequest;
import org.xhy.interfaces.dto.agent.request.UpdateAgentRequest;

import java.util.List;

/**
 * 用户Agent管理
 */
@RestController
@RequestMapping("/agent")
public class PortalAgentController {

    private final AgentAppService agentAppService;

    public PortalAgentController(AgentAppService agentAppService) {
        this.agentAppService = agentAppService;
    }

    /**
     * 创建新Agent
     */
    @PostMapping
    public Result<AgentDTO> createAgent(@RequestBody @Validated CreateAgentRequest request) {
        String userId = UserContext.getCurrentUserId();
        AgentDTO agent = agentAppService.createAgent(request, userId);
        return Result.success(agent);
    }

    /**
     * 获取Agent详情
     */
    @GetMapping("/{agentId}")
    public Result<AgentDTO> getAgent(@PathVariable String agentId) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(agentAppService.getAgent(agentId, userId));
    }

    /**
     * 获取用户的Agent列表，支持可选的状态和名称过滤
     */
    @GetMapping("/user")
    public Result<List<AgentDTO>> getUserAgents(
            SearchAgentsRequest searchAgentsRequest) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(agentAppService.getUserAgents(userId, searchAgentsRequest));
    }

    /**
     * 获取已上架的Agent列表，支持名称搜索
     */
    @GetMapping("/published")
    public Result<List<AgentVersionDTO>> getPublishedAgents(SearchAgentsRequest searchAgentsRequest) {
        return Result.success(agentAppService.getPublishedAgentsByName(searchAgentsRequest));
    }

    /**
     * 更新Agent信息（基本信息和配置合并更新）
     */
    @PutMapping("/{agentId}")
    public Result<AgentDTO> updateAgent(@PathVariable String agentId,
            @RequestBody @Validated UpdateAgentRequest request) {
        String userId = UserContext.getCurrentUserId();
        request.setAgentId(agentId);
        return Result.success(agentAppService.updateAgent(request, userId));
    }

    /**
     * 切换Agent的启用/禁用状态
     */
    @PutMapping("/{agentId}/toggle-status")
    public Result<AgentDTO> toggleAgentStatus(@PathVariable String agentId) {
        return Result.success(agentAppService.toggleAgentStatus(agentId));
    }

    /**
     * 删除Agent
     */
    @DeleteMapping("/{agentId}")
    public Result<Void> deleteAgent(@PathVariable String agentId) {
        String userId = UserContext.getCurrentUserId();
        agentAppService.deleteAgent(agentId, userId);
        return Result.success(null);
    }

    /**
     * 发布Agent版本
     */
    @PostMapping("/{agentId}/publish")
    public Result<AgentVersionDTO> publishAgentVersion(@PathVariable String agentId,
            @RequestBody PublishAgentVersionRequest request) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(agentAppService.publishAgentVersion(agentId, request, userId));
    }

    /**
     * 获取Agent的所有版本
     */
    @GetMapping("/{agentId}/versions")
    public Result<List<AgentVersionDTO>> getAgentVersions(@PathVariable String agentId) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(agentAppService.getAgentVersions(agentId, userId));
    }

    /**
     * 获取Agent的特定版本
     */
    @GetMapping("/{agentId}/versions/{versionNumber}")
    public Result<AgentVersionDTO> getAgentVersion(@PathVariable String agentId,
            @PathVariable String versionNumber) {
        return Result.success(agentAppService.getAgentVersion(agentId, versionNumber));
    }

    /**
     * 获取Agent的最新版本
     */
    @GetMapping("/{agentId}/versions/latest")
    public Result<AgentVersionDTO> getLatestAgentVersion(@PathVariable String agentId) {
        return Result.success(agentAppService.getLatestAgentVersion(agentId));
    }
}