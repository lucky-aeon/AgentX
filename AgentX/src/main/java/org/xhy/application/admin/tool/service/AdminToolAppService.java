package org.xhy.application.admin.tool.service;

import org.springframework.stereotype.Service;
import org.xhy.application.tool.service.ToolAppService;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.service.ToolDomainService;
import org.xhy.domain.tool.service.ToolStateDomainService;
import org.xhy.infrastructure.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AdminToolAppService {

    private static final Logger logger = LoggerFactory.getLogger(AdminToolAppService.class);

    private final ToolDomainService toolDomainService;
    private final ToolStateDomainService toolStateService;
    private final ToolAppService toolAppService;

    public AdminToolAppService(ToolDomainService toolDomainService, ToolStateDomainService toolStateService,
            ToolAppService toolAppService) {
        this.toolDomainService = toolDomainService;
        this.toolStateService = toolStateService;
        this.toolAppService = toolAppService;
    }

    /** 该接口用于管理员修改状态，如果当前工具是人工审核则需要
     *
     * @param toolId 工具 id
     * @param status 状态
     * @param rejectReason 拒绝原因 */
    public void updateToolStatus(String toolId, ToolStatus status, String rejectReason) {

        ToolEntity tool = toolDomainService.getTool(toolId);

        // 如果状态一致
        if (tool.getStatus().equals(status)) {
            throw new BusinessException("状态一致,不可修改");
        }

        if (tool.getStatus() == ToolStatus.MANUAL_REVIEW && status == ToolStatus.APPROVED) {
            // 人工审核通过，调用状态服务处理
            String approvedToolId = toolStateService.manualReviewComplete(tool, true);
            // 审核通过后，手动触发自动安装
            toolAppService.autoInstallApprovedTool(approvedToolId);
        } else if (status == ToolStatus.FAILED) {
            // 审核失败处理
            tool.setFailedStepStatus(tool.getStatus());
            toolDomainService.updateFailedToolStatus(tool.getId(), tool.getStatus(), rejectReason);
        } else if (status == ToolStatus.APPROVED) {
            // 其他状态直接变为APPROVED状态时，也需要自动安装
            toolDomainService.updateApprovedToolStatus(tool.getId(), status);
            toolAppService.autoInstallApprovedTool(toolId);
        } else {
            // 其他状态变更
            toolDomainService.updateApprovedToolStatus(tool.getId(), status);
        }
    }
}
