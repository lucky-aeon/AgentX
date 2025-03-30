package org.xhy.interfaces.api.admin;

import org.springframework.web.bind.annotation.*;
import org.xhy.application.agent.service.AgentAppService;
import org.xhy.application.agent.dto.AgentVersionDTO;
import org.xhy.domain.agent.constant.PublishStatus;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.agent.ReviewAgentVersionRequest;

import java.util.List;
/**
 * 管理员Agent管理
 * 负责处理管理员对Agent的管理操作，如审核、查看待审核列表等
 */
@RestController
@RequestMapping("/admin/agent")
public class AdminAgentController {

    private final AgentAppService agentAppService;

    public AdminAgentController(AgentAppService agentAppService) {
        this.agentAppService = agentAppService;
    }

    /**
     * 获取版本列表，可按状态筛选
     *
     * @param status 版本状态（可选）：REVIEWING - 审核中，PUBLISHED - 已发布，REJECTED - 已拒绝，REMOVED - 已下架
     * @return 符合条件的版本列表（每个助理只返回最新版本）
     */
    @GetMapping("/versions")
    public Result<List<AgentVersionDTO>> getVersions(@RequestParam(required = false) Integer status) {
        // 根据状态参数获取对应的版本列表

        return Result.success(agentAppService.getVersionsByStatus(PublishStatus.fromCode(status)));
    }

    /**
     * 更新版本状态（包括审核通过/拒绝/下架等操作）
     *
     * @param versionId 版本ID
     * @param status 目标状态: PUBLISHED, REJECTED, REMOVED
     * @param reason 原因（拒绝时需要提供）
     * @return 更新后的版本
     */
    @PostMapping("/versions/{versionId}/status")
    public Result<AgentVersionDTO> updateVersionStatus(
            @PathVariable String versionId,
            @RequestParam Integer status,
            @RequestParam(required = false) String reason) {

        PublishStatus publishStatus = PublishStatus.fromCode(status);

        // 如果是拒绝操作，需要检查原因
        if (publishStatus == PublishStatus.REJECTED && (reason == null || reason.isEmpty())) {
            return Result.serverError("拒绝操作需要提供原因");
        }

        // 统一使用reviewAgentVersion接口处理所有状态变更
        ReviewAgentVersionRequest request = new ReviewAgentVersionRequest();
        request.setStatus(publishStatus);

        // 只有在拒绝时设置原因
        if (publishStatus == PublishStatus.REJECTED) {
            request.setRejectReason(reason);
        }


        return Result.success(agentAppService.reviewAgentVersion(versionId, request));
    }
}



