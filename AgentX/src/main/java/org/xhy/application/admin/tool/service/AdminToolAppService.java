package org.xhy.application.admin.tool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.service.ToolDomainService;
import org.xhy.domain.tool.service.ToolStateService;
import org.xhy.infrastructure.exception.BusinessException;

@Service
public class AdminToolAppService {

    private static final Logger logger = LoggerFactory.getLogger(AdminToolAppService.class);

    private final ToolDomainService toolDomainService;
    private final ToolStateService toolStateService;

    public AdminToolAppService(ToolDomainService toolDomainService, ToolStateService toolStateService) {
        this.toolDomainService = toolDomainService;
        this.toolStateService = toolStateService;
    }

    public void updateToolStatus(String toolId, ToolStatus status, String rejectReason) {
        ToolEntity tool = toolDomainService.getTool(toolId);

        if (tool.getStatus().equals(status)) {
            throw new BusinessException("状态一致，不可重复修改");
        }

        if (tool.getStatus() == ToolStatus.MANUAL_REVIEW && status == ToolStatus.APPROVED) {
            // 人工审核通过后不是直接结束，而是转入 PUBLISHING 继续执行发布动作
            toolStateService.manualReviewComplete(tool, true);
        }

        if (status == ToolStatus.FAILED) {
            // 拒绝时把“失败发生在哪个步骤”也记下来，方便前端展示和排错
            tool.setFailedStepStatus(tool.getStatus());
            toolDomainService.updateFailedToolStatus(tool.getId(), tool.getStatus(), rejectReason);
        } else {
            toolDomainService.updateApprovedToolStatus(tool.getId(), status);
        }
    }
}
