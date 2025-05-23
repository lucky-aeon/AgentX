package org.xhy.domain.tool.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.model.ToolVersionEntity;
import org.xhy.domain.tool.repository.ToolRepository;
import org.xhy.domain.tool.repository.ToolVersionRepository;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.utils.JsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.xhy.application.tool.dto.AvailableToolDTO;

/** 工具领域服务 */
@Service
public class ToolDomainService {

    private final ToolRepository toolRepository;
    private final ToolVersionRepository toolVersionRepository;
    private final ToolStateService toolStateService;

    private static final String CUSTOM_TOOL_PREFIX = "custom:";
    private static final String INSTALLED_TOOL_PREFIX = "installed:";

    public ToolDomainService(ToolRepository toolRepository, ToolVersionRepository toolVersionRepository,
            ToolStateService toolStateService) {
        this.toolRepository = toolRepository;
        this.toolVersionRepository = toolVersionRepository;
        this.toolStateService = toolStateService;
    }

    /**
     * 创建工具
     *
     * @param toolEntity 工具实体
     * @return 创建后的工具实体
     */
    @Transactional
    public ToolEntity createTool(ToolEntity toolEntity) {
        // 设置初始状态
        toolEntity.setStatus(ToolStatus.WAITING_REVIEW);

        String mcpServerName = this.getMcpServerName(toolEntity);

        toolEntity.setMcpServerName(mcpServerName);
        // 保存工具
        toolRepository.checkInsert(toolEntity);

        // 提交到状态流转服务进行处理
        toolStateService.submitToolForProcessing(toolEntity);

        return toolEntity;
    }

    public ToolEntity getTool(String toolId, String userId) {
        Wrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaQuery().eq(ToolEntity::getId, toolId)
                .eq(ToolEntity::getUserId, userId);
        ToolEntity toolEntity = toolRepository.selectOne(wrapper);
        if (toolEntity == null) {
            throw new BusinessException("工具不存在: " + toolId);
        }
        return toolEntity;
    }

    public List<ToolEntity> getUserTools(String userId) {
        LambdaQueryWrapper<ToolEntity> queryWrapper = Wrappers.<ToolEntity>lambdaQuery()
                .eq(ToolEntity::getUserId, userId).orderByDesc(ToolEntity::getUpdatedAt);
        return toolRepository.selectList(queryWrapper);
    }

    public ToolEntity updateApprovedToolStatus(String toolId, ToolStatus status) {

        LambdaUpdateWrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaUpdate()
                .eq(ToolEntity::getId, toolId).set(ToolEntity::getStatus, status);
        toolRepository.checkedUpdate(wrapper);
        return toolRepository.selectById(toolId);
    }

    public ToolEntity updateTool(ToolEntity toolEntity) {
        /**
         * 修改 name/description/icon/labels只触发人工审核状态 修改 upload_url/upload_command触发整个状态扭转
         */
        // 获取原工具信息
        ToolEntity oldTool = toolRepository.selectById(toolEntity.getId());
        if (oldTool == null) {
            throw new BusinessException("工具不存在: " + toolEntity.getId());
        }

        // 检查是否修改了URL或安装命令
        boolean needStateTransition = false;
        if ((toolEntity.getUploadUrl() != null && !toolEntity.getUploadUrl().equals(oldTool.getUploadUrl()))
                || (toolEntity.getInstallCommand() != null
                        && !toolEntity.getInstallCommand().equals(oldTool.getInstallCommand()))) {
            needStateTransition = true;
            String mcpServerName = this.getMcpServerName(toolEntity);
            toolEntity.setMcpServerName(mcpServerName);
            toolEntity.setStatus(ToolStatus.WAITING_REVIEW);
        } else {
            // 只修改了信息，设置为人工审核状态
            toolEntity.setStatus(ToolStatus.MANUAL_REVIEW);
        }

        // 更新工具
        LambdaUpdateWrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaUpdate()
                .eq(ToolEntity::getId, toolEntity.getId())
                .eq(toolEntity.needCheckUserId(), ToolEntity::getUserId, toolEntity.getUserId());
        toolRepository.update(toolEntity, wrapper);

        // 如果需要状态流转，提交到状态流转服务
        if (needStateTransition) {
            toolStateService.submitToolForProcessing(toolEntity);
        }

        return toolEntity;
    }

    @Transactional
    public void deleteTool(String toolId, String userId) {
        // 删除工具
        Wrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaQuery().eq(ToolEntity::getId, toolId)
                .eq(ToolEntity::getUserId, userId);

        // 删除工具版本
        Wrapper<ToolVersionEntity> versionWrapper = Wrappers.<ToolVersionEntity>lambdaQuery()
                .eq(ToolVersionEntity::getToolId, toolId);
        toolRepository.checkedDelete(wrapper);
        toolVersionRepository.delete(versionWrapper);
        // todo xhy 删除 mcp_community 仓库
    }

    public ToolEntity getTool(String toolId) {
        Wrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaQuery().eq(ToolEntity::getId, toolId);
        ToolEntity toolEntity = toolRepository.selectOne(wrapper);
        if (toolEntity == null) {
            throw new BusinessException("工具不存在: " + toolId);
        }
        return toolEntity;
    }

    public ToolEntity updateFailedToolStatus(String toolId, ToolStatus failedStepStatus, String rejectReason) {
        LambdaUpdateWrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaUpdate()
                .eq(ToolEntity::getId, toolId)
                .set(ToolEntity::getFailedStepStatus, failedStepStatus)
                .set(ToolEntity::getRejectReason, rejectReason)
                .set(ToolEntity::getStatus, ToolStatus.FAILED);
        toolRepository.checkedUpdate(wrapper);
        return toolRepository.selectById(toolId);
    }

    /**
     * 校验工具列表的可用性
     *
     * @param prefixedToolIds 带前缀的工具ID列表
     * @param userId          用户ID
     */
    public void validTools(List<String> prefixedToolIds, String userId) {
        if (prefixedToolIds == null || prefixedToolIds.isEmpty()) {
            return; // 没有工具需要校验
        }

        for (String prefixedId : prefixedToolIds) {
            if (prefixedId.startsWith(CUSTOM_TOOL_PREFIX)) {
                String toolId = prefixedId.substring(CUSTOM_TOOL_PREFIX.length());
                // 校验自定义工具
                ToolEntity tool = toolRepository.selectOne(
                        Wrappers.<ToolEntity>lambdaQuery().eq(ToolEntity::getId, toolId).eq(ToolEntity::getUserId,
                                userId));
                if (tool == null) {
                    throw new BusinessException("自定义工具不存在或不属于该用户: " + toolId);
                }
                if (tool.getStatus() != ToolStatus.APPROVED) {
                    throw new BusinessException("自定义工具 '" + tool.getName() + "' 未审核通过，无法使用。");
                }
            } else if (prefixedId.startsWith(INSTALLED_TOOL_PREFIX)) {
                String toolVersionId = prefixedId.substring(INSTALLED_TOOL_PREFIX.length());
                // 校验已安装的工具（工具版本）
                ToolVersionEntity toolVersion = toolVersionRepository.selectById(toolVersionId);
                if (toolVersion == null) {
                    throw new BusinessException("已安装的工具版本不存在: " + toolVersionId);
                }

                if (!toolVersion.getPublicStatus()) {
                    throw new BusinessException("工具版本已私密: " + toolVersion.getName());
                }
            } else {
                throw new BusinessException("无法识别的工具ID前缀: " + prefixedId);
            }
        }
    }

    private String getMcpServerName(ToolEntity tool) {
        if (tool == null) {
            return null;
        }
        Map<String, Object> installCommand = tool.getInstallCommand();

        @SuppressWarnings("unchecked")
        Map<String, Object> mcpServers = (Map<String, Object>) installCommand.get("mcpServers");
        if (mcpServers == null || mcpServers.isEmpty()) {
            throw new BusinessException("工具ID: " + tool.getId() + " 安装命令中mcpServers为空。");
        }

        // 获取第一个key作为工具名称
        String toolName = mcpServers.keySet().iterator().next();
        if (toolName == null || toolName.isEmpty()) {
            throw new BusinessException("工具ID: " + tool.getId() + " 无法从安装命令中获取工具名称。");
        }
        return toolName;
    }
}