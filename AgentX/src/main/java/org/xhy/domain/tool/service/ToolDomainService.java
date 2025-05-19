package org.xhy.domain.tool.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.model.ToolVersionEntity;
import org.xhy.domain.tool.repository.ToolRepository;
import org.xhy.domain.tool.repository.ToolVersionRepository;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.interfaces.dto.tool.request.QueryToolRequest;

import java.util.List;

/**
 * 工具领域服务
 */
@Service
public class ToolDomainService {

    private final ToolRepository toolRepository;
    private final ToolVersionRepository toolVersionRepository;
    private final ToolStateService toolStateService;

    public ToolDomainService(ToolRepository toolRepository, 
                             ToolVersionRepository toolVersionRepository,
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
        
        // 保存工具
        toolRepository.checkInsert(toolEntity);
        
        // 提交到状态流转服务进行处理
        toolStateService.submitTool(toolEntity.getId());
        
        return toolEntity;
    }

    public ToolEntity getTool(String toolId, String userId) {
        Wrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaQuery()
                .eq(ToolEntity::getId, toolId)
                .eq(ToolEntity::getUserId, userId);
        ToolEntity toolEntity = toolRepository.selectOne(wrapper);
        if (toolEntity == null) {
            throw new BusinessException("工具不存在: " + toolId);
        }
        return toolEntity;
    }

    public List<ToolEntity> getUserTools(String userId) {
        LambdaQueryWrapper<ToolEntity> queryWrapper = Wrappers.<ToolEntity>lambdaQuery()
                .eq(ToolEntity::getUserId, userId)
                .orderByDesc(ToolEntity::getUpdatedAt);
        return toolRepository.selectList(queryWrapper);
    }

    public ToolEntity updateTool(ToolEntity toolEntity) {
        /**
         * 修改 name/description/icon/labels只触发人工审核状态
         * 修改 upload_url/upload_command触发整个状态扭转
         */
        // 获取原工具信息
        ToolEntity oldTool = toolRepository.selectById(toolEntity.getId());
        if (oldTool == null) {
            throw new BusinessException("工具不存在: " + toolEntity.getId());
        }
        
        // 检查是否修改了URL或安装命令
        boolean needStateTransition = false;
        if ((toolEntity.getUploadUrl() != null && !toolEntity.getUploadUrl().equals(oldTool.getUploadUrl())) ||
            (toolEntity.getInstallCommand() != null && !toolEntity.getInstallCommand().equals(oldTool.getInstallCommand()))) {
            needStateTransition = true;
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
            toolStateService.submitTool(toolEntity.getId());
        }
        
        return toolEntity;
    }

    @Transactional
    public void deleteTool(String toolId, String userId) {
        // 删除工具
        Wrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaQuery()
                .eq(ToolEntity::getId, toolId)
                .eq(ToolEntity::getUserId, userId);

        // 删除工具版本
        Wrapper<ToolVersionEntity> versionWrapper = Wrappers.<ToolVersionEntity>lambdaQuery()
                .eq(ToolVersionEntity::getToolId, toolId);
        toolRepository.checkedDelete(wrapper);
        toolVersionRepository.delete(versionWrapper);
    }

    public ToolEntity getTool(String toolId) {
        Wrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaQuery()
                .eq(ToolEntity::getId, toolId);
        ToolEntity toolEntity = toolRepository.selectOne(wrapper);
        if (toolEntity == null) {
            throw new BusinessException("工具不存在: " + toolId);
        }
        return toolEntity;
    }
    
    /**
     * 人工审核通过
     */
    public ToolEntity approveManualReview(String toolId, String operatorId) {
        toolStateService.approveManualReview(toolId);
        return getTool(toolId);
    }
    
    /**
     * 重新开始状态流转
     */
    public ToolEntity restartStateTransition(String toolId, String userId) {
        // 验证工具属于用户
        getTool(toolId, userId);
        
        // 重新开始状态流转
        toolStateService.restartProcess(toolId);
        
        return getTool(toolId);
    }
}