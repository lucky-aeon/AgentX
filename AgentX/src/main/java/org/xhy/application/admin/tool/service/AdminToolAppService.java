package org.xhy.application.admin.tool.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.tool.assembler.ToolAssembler;
import org.xhy.application.tool.service.ToolAppService;
import org.xhy.application.tool.dto.ToolWithUserDTO;
import org.xhy.application.tool.dto.ToolStatisticsDTO;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.constant.ToolType;
import org.xhy.domain.tool.constant.UploadType;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.service.ToolDomainService;
import org.xhy.domain.tool.service.ToolStateDomainService;
import org.xhy.interfaces.dto.tool.request.CreateToolRequest;
import org.xhy.interfaces.dto.tool.request.QueryToolRequest;
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

    /** 创建官方工具（管理员专用）
     * 
     * @param request 工具创建请求
     * @param userId 创建者用户ID
     * @return 工具ID */
    @Transactional
    public String createOfficialTool(CreateToolRequest request, String userId) {
        logger.info("管理员创建官方工具: userId={}, toolName={}", userId, request.getName());

        ToolEntity entity = ToolAssembler.toEntity(request, userId);
        entity.setIsOffice(true);

        // 保存工具
        ToolEntity createdTool = toolDomainService.createTool(entity);
        String toolId = createdTool.getId();

        logger.info("官方工具创建成功: toolId={}", toolId);
        return toolId;
    }

    /** 该接口用于管理员修改状态，如果当前工具是人工审核则需要
     *
     * @param toolId 工具 id
     * @param status 状态
     * @param rejectReason 拒绝原因 */
    public void updateToolStatus(String toolId, ToolStatus status, String rejectReason) {

        ToolEntity tool = toolDomainService.getTool(toolId);

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

    /** 分页查询工具列表（管理员使用，包含用户信息）
     * 
     * @param queryToolRequest 查询条件
     * @return 工具分页数据（包含用户信息） */
    public Page<ToolWithUserDTO> getTools(QueryToolRequest queryToolRequest) {
        Page<ToolEntity> page = toolDomainService.getTools(queryToolRequest);
        return toolDomainService.getToolsWithUserInfo(page);
    }

    /** 获取工具统计信息
     * 
     * @return 工具统计数据 */
    public ToolStatisticsDTO getToolStatistics() {
        return toolDomainService.getToolStatistics();
    }

    /** 更新工具全局状态
     * 
     * @param toolId 工具ID
     * @param isGlobal 是否为全局工具 */
    @Transactional
    public void updateToolGlobalStatus(String toolId, Boolean isGlobal) {
        logger.info("更新工具全局状态: toolId={}, isGlobal={}", toolId, isGlobal);

        // 检查工具是否存在
        ToolEntity tool = toolDomainService.getTool(toolId);
        if (tool == null) {
            throw new BusinessException("工具不存在: " + toolId);
        }

        // 使用专门的方法更新全局状态，不触发审核流程
        toolDomainService.updateToolGlobalStatus(toolId, isGlobal);

        logger.info("工具全局状态更新成功: toolId={}, isGlobal={}", toolId, isGlobal);
    }
}
