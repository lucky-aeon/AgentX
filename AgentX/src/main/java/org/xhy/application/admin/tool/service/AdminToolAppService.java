package org.xhy.application.admin.tool.service;

import org.springframework.stereotype.Service;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.service.ToolDomainService;
import org.xhy.domain.tool.service.ToolStateService;
import org.xhy.infrastructure.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AdminToolAppService {

    private static final Logger logger = LoggerFactory.getLogger(AdminToolAppService.class);

    private final ToolDomainService toolDomainService;
    private final ToolStateService toolStateService;

    public AdminToolAppService(ToolDomainService toolDomainService, ToolStateService toolStateService) {
        this.toolDomainService = toolDomainService;
        this.toolStateService = toolStateService;
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
            toolStateService.manualReviewComplete(tool, true);
        }

        if (status == ToolStatus.FAILED) {
            tool.setFailedStepStatus(tool.getStatus());
            toolDomainService.updateFailedToolStatus(tool.getId(), tool.getStatus(), rejectReason);
        } else {
            toolDomainService.updateApprovedToolStatus(tool.getId(), status);
        }
    }
}
