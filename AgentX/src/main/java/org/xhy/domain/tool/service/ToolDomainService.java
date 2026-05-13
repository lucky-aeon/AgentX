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

import java.util.List;
import java.util.Set;

@Service
public class ToolDomainService {

    private final ToolRepository toolRepository;
    private final ToolVersionRepository toolVersionRepository;
    private final ToolStateService toolStateService;

    public ToolDomainService(ToolRepository toolRepository, ToolVersionRepository toolVersionRepository,
            ToolStateService toolStateService) {
        this.toolRepository = toolRepository;
        this.toolVersionRepository = toolVersionRepository;
        this.toolStateService = toolStateService;
    }

    @Transactional
    public ToolEntity createTool(ToolEntity toolEntity) {
        toolEntity.setStatus(ToolStatus.WAITING_REVIEW);
        toolRepository.checkInsert(toolEntity);

        // 工具创建后立刻提交到状态流转服务，
        // 后续的 GitHub 校验、部署、拉取工具定义都走异步流程
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
        LambdaUpdateWrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaUpdate().eq(ToolEntity::getId, toolId)
                .set(ToolEntity::getStatus, status);
        toolRepository.checkedUpdate(wrapper);
        return toolRepository.selectById(toolId);
    }

    public ToolEntity updateTool(ToolEntity toolEntity) {
        ToolEntity oldTool = toolRepository.selectById(toolEntity.getId());
        if (oldTool == null) {
            throw new BusinessException("工具不存在: " + toolEntity.getId());
        }

        boolean needStateTransition = false;
        if ((toolEntity.getUploadUrl() != null && !toolEntity.getUploadUrl().equals(oldTool.getUploadUrl()))
                || (toolEntity.getInstallCommand() != null
                        && !toolEntity.getInstallCommand().equals(oldTool.getInstallCommand()))) {
            needStateTransition = true;
            // 源地址或安装命令变化，说明工具执行能力可能变化，需要重跑完整自动化流程
            toolEntity.setStatus(ToolStatus.WAITING_REVIEW);
        } else {
            // 只改文案、图标、标签等展示信息时，只进入人工审核，不重跑部署流程
            toolEntity.setStatus(ToolStatus.MANUAL_REVIEW);
        }

        LambdaUpdateWrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaUpdate()
                .eq(ToolEntity::getId, toolEntity.getId())
                .eq(toolEntity.needCheckUserId(), ToolEntity::getUserId, toolEntity.getUserId());
        toolRepository.update(toolEntity, wrapper);

        if (needStateTransition) {
            toolStateService.submitToolForProcessing(toolEntity);
        }

        return toolEntity;
    }

    @Transactional
    public void deleteTool(String toolId, String userId) {
        Wrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaQuery().eq(ToolEntity::getId, toolId)
                .eq(ToolEntity::getUserId, userId);
        Wrapper<ToolVersionEntity> versionWrapper = Wrappers.<ToolVersionEntity>lambdaQuery()
                .eq(ToolVersionEntity::getToolId, toolId);
        toolRepository.checkedDelete(wrapper);
        toolVersionRepository.delete(versionWrapper);
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
        LambdaUpdateWrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaUpdate().eq(ToolEntity::getId, toolId)
                .set(ToolEntity::getFailedStepStatus, failedStepStatus).set(ToolEntity::getRejectReason, rejectReason)
                .set(ToolEntity::getStatus, ToolStatus.FAILED);
        toolRepository.checkedUpdate(wrapper);
        return toolRepository.selectById(toolId);
    }

    public List<ToolEntity> findProcessingTools() {
        Set<ToolStatus> terminalStatuses = new java.util.HashSet<>(ToolStatus.getTerminalStatuses());
        terminalStatuses.add(ToolStatus.MANUAL_REVIEW);
        // 处理中 = 既没结束，也没停在等待管理员判断的人工审核状态
        LambdaQueryWrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaQuery()
                .notIn(ToolEntity::getStatus, terminalStatuses);
        return toolRepository.selectList(wrapper);
    }
}
