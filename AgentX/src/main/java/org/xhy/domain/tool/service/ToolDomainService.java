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
import org.xhy.domain.tool.model.UserToolEntity;
import org.xhy.domain.tool.repository.ToolRepository;
import org.xhy.domain.tool.repository.ToolVersionRepository;
import org.xhy.domain.tool.repository.UserToolRepository;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 工具领域服务 */
@Service
public class ToolDomainService {

    private final ToolRepository toolRepository;
    private final ToolVersionRepository toolVersionRepository;
    private final ToolStateDomainService toolStateService;
    private final UserToolRepository userToolRepository;

    public ToolDomainService(ToolRepository toolRepository, ToolVersionRepository toolVersionRepository,
            ToolStateDomainService toolStateService, UserToolRepository userToolRepository) {
        this.toolRepository = toolRepository;
        this.toolVersionRepository = toolVersionRepository;
        this.toolStateService = toolStateService;
        this.userToolRepository = userToolRepository;
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


    /**
     * 检查工具版本是否已安装
     * 
     * 校验的是 toolversionId，并非是 tooId
     * 
     * @param toolVersionIds 工具版本id列表
     * @param userId 用户id
     */
    public void checkToolAvailability(List<String> toolVersionIds, String userId) {
        if (toolVersionIds == null || toolVersionIds.isEmpty()) {
            return;
        }
        // 检查工具版本是否存在
        List<ToolVersionEntity> toolVersionEntities = toolVersionRepository.selectList(Wrappers.<ToolVersionEntity>lambdaQuery()
                        .in(ToolVersionEntity::getId, toolVersionIds)
                        .eq(ToolVersionEntity::getUserId, userId));

        //  toolVersionEntities 转 map,key为 id，value 为本身
        Map<String, ToolVersionEntity> toolVersionMap = toolVersionEntities.stream()
                .collect(Collectors.toMap(ToolVersionEntity::getId,Function.identity()));
        
        List<String> toolIds = new ArrayList<>();
        toolVersionIds.forEach(toolVersionId -> {
            ToolVersionEntity toolVersionEntity = toolVersionMap.get(toolVersionId);
            if (toolVersionEntity == null) {
                throw new BusinessException("工具缺失，请联系作者操作");
            }

            if (!toolVersionEntity.getUserId().equals(userId) && !toolVersionEntity.getPublicStatus()){
                throw new BusinessException("工具缺失，请联系作者操作");

            }
            toolIds.add(toolVersionEntity.getToolId());
        });

       // 检查这些工具是否是自己已安装的
        List<UserToolEntity> userToolEntities = userToolRepository.selectList(Wrappers.<UserToolEntity>lambdaQuery()
                .in(UserToolEntity::getToolId, toolIds)
                .eq(UserToolEntity::getUserId, userId));

        // 转 map
        Map<String, UserToolEntity> userToolMap = userToolEntities.stream()
                .collect(Collectors.toMap(UserToolEntity::getToolId, Function.identity()));

        
        toolIds.forEach(toolId -> {
            UserToolEntity userToolEntity = userToolMap.get(toolId);
            if (userToolEntity == null) {
                throw new BusinessException("工具缺失，请联系作者操作");
            }
        });
    }
}