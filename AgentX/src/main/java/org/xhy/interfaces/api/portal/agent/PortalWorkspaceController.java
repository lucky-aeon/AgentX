package org.xhy.interfaces.api.portal.agent;

import io.jsonwebtoken.lang.Maps;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.agent.service.AgentWorkspaceAppService;
import org.xhy.application.agent.dto.AgentDTO;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Agent工作区
 */
@RestController
@RequestMapping("/agent/workspace")
public class PortalWorkspaceController {

    private final AgentWorkspaceAppService agentWorkspaceAppService;

    public PortalWorkspaceController(AgentWorkspaceAppService agentWorkspaceAppService) {
        this.agentWorkspaceAppService = agentWorkspaceAppService;
    }

    /**
     * 获取工作区下的助理
     * 
     * @return
     */
    @GetMapping("/agents")
    public Result<List<AgentDTO>> getAgents() {
        String userId = UserContext.getCurrentUserId();
        return Result.success(agentWorkspaceAppService.getAgents(userId));
    }


    /**
     * 删除工作区中的助理
     * 
     * @param id 助理id
     */
    @DeleteMapping("/agents/{id}")
    public Result<Void> deleteAgent(@PathVariable String id) {
        String userId = UserContext.getCurrentUserId(); 
        agentWorkspaceAppService.deleteAgent(id,userId);
        return Result.success();
    }

    /**
     * 设置agent的模型
     * @param modelId 模型id
     * @param agentId agentId
     * @return
     */
    @PutMapping("/{agentId}/model/{modelId}")
    public Result<Void> saveModelId(@PathVariable String modelId,@PathVariable String agentId){
        String userId = UserContext.getCurrentUserId();
        agentWorkspaceAppService.saveModel(agentId,userId,modelId);
        return Result.success();
    }

    /**
     * 根据agentId和userId获取对应的modelId
     * @param agentId agentId
     * @return
     */
    @GetMapping("/{agentId}/model")
    public Result<Map<String, Object>> getConfiguredModelId(@PathVariable String agentId){
        String userId = UserContext.getCurrentUserId();
        String modelId = agentWorkspaceAppService.getConfiguredModelId(agentId,userId);
        Map<String, Object> result = new HashMap<>();
        result.put("modelId", modelId);
        return Result.success(result);
    }
}