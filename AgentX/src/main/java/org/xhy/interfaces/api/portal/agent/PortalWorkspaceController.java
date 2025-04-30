package org.xhy.interfaces.api.portal.agent;

import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.agent.dto.AgentDTO;
import org.xhy.application.agent.service.AgentWorkspaceAppService;
import org.xhy.domain.agent.model.LLMModelConfig;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.agent.request.UpdateModelConfigRequest;

/** Agent工作区 */
@RestController
@RequestMapping("/agent/workspace")
public class PortalWorkspaceController {

    private final AgentWorkspaceAppService agentWorkspaceAppService;

    public PortalWorkspaceController(AgentWorkspaceAppService agentWorkspaceAppService) {
        this.agentWorkspaceAppService = agentWorkspaceAppService;
    }

    /** 获取工作区下的助理
     *
     * @return */
    @GetMapping("/agents")
    public Result<List<AgentDTO>> getAgents() {
        String userId = UserContext.getCurrentUserId();
        return Result.success(agentWorkspaceAppService.getAgents(userId));
    }

    /** 删除工作区中的助理
     *
     * @param id 助理id */
    @DeleteMapping("/agents/{id}")
    public Result<Void> deleteAgent(@PathVariable String id) {
        String userId = UserContext.getCurrentUserId();
        agentWorkspaceAppService.deleteAgent(id, userId);
        return Result.success();
    }

    /** 设置agent的模型配置
     *
     * @param config 模型配置
     * @param agentId agentId
     * @return */
    @PutMapping("/{agentId}/model/config")
    public Result<Void> saveModelConfig(@RequestBody @Validated UpdateModelConfigRequest config,
            @PathVariable String agentId) {
        String userId = UserContext.getCurrentUserId();
        agentWorkspaceAppService.updateModelConfig(agentId, userId, config);
        return Result.success();
    }

    /** 根据agentId和userId获取对应的modelId
     *
     * @param agentId agentId
     * @return */
    @GetMapping("/{agentId}/model-config")
    public Result<LLMModelConfig> getConfiguredModelId(@PathVariable String agentId) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(agentWorkspaceAppService.getConfiguredModelId(agentId, userId));
    }
}
