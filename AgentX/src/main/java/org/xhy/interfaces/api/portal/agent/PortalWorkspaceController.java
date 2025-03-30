package org.xhy.interfaces.api.portal.agent;

import org.springframework.web.bind.annotation.*;
import org.xhy.application.agent.service.AgentWorkspaceAppService;
import org.xhy.application.agent.dto.AgentDTO;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;

import java.util.List;

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

}