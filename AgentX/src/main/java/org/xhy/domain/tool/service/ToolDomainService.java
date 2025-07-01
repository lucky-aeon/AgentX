package org.xhy.domain.tool.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.stereotype.Service;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.model.ToolOperationResult;
import org.xhy.domain.tool.model.ToolVersionEntity;
import org.xhy.domain.tool.model.UserToolEntity;
import org.xhy.domain.tool.repository.ToolRepository;
import org.xhy.domain.tool.repository.ToolVersionRepository;
import org.xhy.domain.tool.repository.UserToolRepository;
import org.xhy.domain.user.repository.UserRepository;
import org.xhy.domain.user.model.UserEntity;
import org.xhy.application.tool.dto.ToolWithUserDTO;
import org.xhy.application.tool.dto.ToolStatisticsDTO;
import org.xhy.application.tool.assembler.ToolAssembler;
import org.xhy.interfaces.dto.tool.request.QueryToolRequest;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 工具领域服务 */
@Service
public class ToolDomainService {

    private final ToolRepository toolRepository;
    private final ToolVersionRepository toolVersionRepository;
    private final UserToolRepository userToolRepository;
    private final UserRepository userRepository;

    public ToolDomainService(ToolRepository toolRepository, ToolVersionRepository toolVersionRepository,
            UserToolRepository userToolRepository, UserRepository userRepository) {
        this.toolRepository = toolRepository;
        this.toolVersionRepository = toolVersionRepository;
        this.userToolRepository = userToolRepository;
        this.userRepository = userRepository;
    }

    /** 创建工具
     *
     * @param toolEntity 工具实体
     * @return 工具操作结果 */
    public ToolOperationResult createTool(ToolEntity toolEntity) {
        // 设置初始状态
        toolEntity.setStatus(ToolStatus.WAITING_REVIEW);

        String mcpServerName = this.getMcpServerName(toolEntity);
        toolEntity.setMcpServerName(mcpServerName);

        // 校验用户创建的工具MCP名称不能重复
        this.validateMcpServerNameUnique(mcpServerName, toolEntity.getUserId(), null);

        // 保存工具
        toolRepository.checkInsert(toolEntity);

        // 返回需要状态转换的结果
        return ToolOperationResult.withTransition(toolEntity);
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

    public ToolOperationResult updateTool(ToolEntity toolEntity) {
        /** 修改 name/description/icon/labels只触发人工审核状态 修改 upload_url/upload_command触发整个状态扭转 */
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
            
            // 校验更新后的MCP名称不能与用户其他工具重复
            this.validateMcpServerNameUnique(mcpServerName, toolEntity.getUserId(), toolEntity.getId());
            
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

        // 返回操作结果
        return ToolOperationResult.of(toolEntity, needStateTransition);
    }

    public void deleteTool(String toolId, String userId) {

        // 删除工具
        Wrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaQuery().eq(ToolEntity::getId, toolId)
                .eq(ToolEntity::getUserId, userId);

        // 删除当前用户安装的该工具
        Wrapper<UserToolEntity> userToolWrapper = Wrappers.<UserToolEntity>lambdaQuery()
                .eq(UserToolEntity::getToolId, toolId).eq(UserToolEntity::getUserId, userId);

        toolRepository.checkedDelete(wrapper);
        userToolRepository.delete(userToolWrapper);
        // 这里应该删除 mcp community github repo，但是删不干净，索性就不删
        // 用户可以自行修改工具名称，修改后之前的工具名称不记录，因此就算删除，之前的仓库无记录删不了
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

    /** 更新工具实体
     * 
     * @param toolEntity 工具实体
     * @return 更新后的工具实体 */
    public ToolEntity updateToolEntity(ToolEntity toolEntity) {
        if (toolEntity == null || toolEntity.getId() == null) {
            throw new BusinessException("工具实体或工具ID不能为空");
        }
        toolRepository.updateById(toolEntity);
        return toolEntity;
    }

    /** 处理人工审核完成
     *
     * @param tool 工具实体
     * @param approved 审核结果，true表示批准，false表示拒绝
     * @return 返回工具ID，方便调用方进行后续处理 */
    public String manualReviewComplete(ToolEntity tool, boolean approved) {
        if (tool == null) {
            throw new BusinessException("工具不存在");
        }

        String toolId = tool.getId();
        if (tool.getStatus() != ToolStatus.MANUAL_REVIEW) {
            throw new BusinessException("工具ID: " + toolId + " 当前状态不是MANUAL_REVIEW，无法进行人工审核操作");
        }

        if (approved) {
            tool.setStatus(ToolStatus.APPROVED);
            updateToolEntity(tool);
        } else {
            tool.setStatus(ToolStatus.FAILED);
            updateToolEntity(tool);
        }

        return toolId;
    }

    /** 转换工具状态到指定状态
     *
     * @param tool 工具实体
     * @param targetStatus 目标状态 */
    public void transitionToStatus(ToolEntity tool, ToolStatus targetStatus) {
        if (tool == null) {
            throw new BusinessException("工具不存在");
        }

        ToolStatus currentStatus = tool.getStatus();
        if (currentStatus == targetStatus) {
            return; // 状态相同，无需转换
        }

        tool.setStatus(targetStatus);
        updateToolEntity(tool);
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

    public List<ToolEntity> getByIds(List<String> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return new ArrayList<>();
        }
        return toolRepository.selectByIds(toolIds);
    }

    /** 分页查询工具列表
     * 
     * @param queryToolRequest 查询条件
     * @return 工具分页数据 */
    public Page<ToolEntity> getTools(QueryToolRequest queryToolRequest) {
        LambdaQueryWrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaQuery();

        // 关键词搜索：工具名称、描述
        if (queryToolRequest.getKeyword() != null && !queryToolRequest.getKeyword().trim().isEmpty()) {
            String keyword = queryToolRequest.getKeyword().trim();
            wrapper.and(w -> w.like(ToolEntity::getName, keyword).or().like(ToolEntity::getDescription, keyword));
        }

        // 兼容原有字段
        if (queryToolRequest.getToolName() != null && !queryToolRequest.getToolName().trim().isEmpty()) {
            wrapper.like(ToolEntity::getName, queryToolRequest.getToolName().trim());
        }

        // 状态筛选
        if (queryToolRequest.getStatus() != null) {
            wrapper.eq(ToolEntity::getStatus, queryToolRequest.getStatus());
        }

        // 是否官方工具筛选
        if (queryToolRequest.getIsOffice() != null) {
            wrapper.eq(ToolEntity::getIsOffice, queryToolRequest.getIsOffice());
        }

        // 按创建时间倒序排列
        wrapper.orderByDesc(ToolEntity::getCreatedAt);

        // 分页查询
        long current = queryToolRequest.getPage() != null ? queryToolRequest.getPage().longValue() : 1L;
        long size = queryToolRequest.getPageSize() != null ? queryToolRequest.getPageSize().longValue() : 15L;
        Page<ToolEntity> page = new Page<>(current, size);
        return toolRepository.selectPage(page, wrapper);
    }

    /** 获取带用户信息的工具分页数据
     * 
     * @param toolPage 工具分页数据
     * @return 包含用户信息的工具分页数据 */
    public Page<ToolWithUserDTO> getToolsWithUserInfo(Page<ToolEntity> toolPage) {
        if (toolPage.getRecords().isEmpty()) {
            Page<ToolWithUserDTO> result = new Page<>();
            result.setCurrent(toolPage.getCurrent());
            result.setSize(toolPage.getSize());
            result.setTotal(toolPage.getTotal());
            result.setRecords(new ArrayList<>());
            return result;
        }

        // 获取所有用户ID
        List<String> userIds = toolPage.getRecords().stream().map(ToolEntity::getUserId).distinct()
                .collect(Collectors.toList());

        // 批量查询用户信息
        List<UserEntity> users = userRepository.selectBatchIds(userIds);
        Map<String, UserEntity> userMap = users.stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        // 组装结果
        List<ToolWithUserDTO> records = toolPage.getRecords().stream()
                .map(tool -> ToolAssembler.toToolWithUserDTO(tool, userMap.get(tool.getUserId())))
                .collect(Collectors.toList());

        Page<ToolWithUserDTO> result = new Page<>();
        result.setCurrent(toolPage.getCurrent());
        result.setSize(toolPage.getSize());
        result.setTotal(toolPage.getTotal());
        result.setRecords(records);
        return result;
    }

    /** 获取工具统计信息 */
    public ToolStatisticsDTO getToolStatistics() {
        ToolStatisticsDTO statistics = new ToolStatisticsDTO();

        // 总工具数量
        long totalTools = toolRepository.selectCount(null);
        statistics.setTotalTools(totalTools);

        // 待审核工具数量（WAITING_REVIEW状态）
        LambdaQueryWrapper<ToolEntity> pendingWrapper = Wrappers.<ToolEntity>lambdaQuery().eq(ToolEntity::getStatus,
                ToolStatus.WAITING_REVIEW);
        long pendingReviewTools = toolRepository.selectCount(pendingWrapper);
        statistics.setPendingReviewTools(pendingReviewTools);

        // 人工审核工具数量（MANUAL_REVIEW状态）
        LambdaQueryWrapper<ToolEntity> manualWrapper = Wrappers.<ToolEntity>lambdaQuery().eq(ToolEntity::getStatus,
                ToolStatus.MANUAL_REVIEW);
        long manualReviewTools = toolRepository.selectCount(manualWrapper);
        statistics.setManualReviewTools(manualReviewTools);

        // 已通过工具数量（APPROVED状态）
        LambdaQueryWrapper<ToolEntity> approvedWrapper = Wrappers.<ToolEntity>lambdaQuery().eq(ToolEntity::getStatus,
                ToolStatus.APPROVED);
        long approvedTools = toolRepository.selectCount(approvedWrapper);
        statistics.setApprovedTools(approvedTools);

        // 审核失败工具数量（FAILED状态）
        LambdaQueryWrapper<ToolEntity> failedWrapper = Wrappers.<ToolEntity>lambdaQuery().eq(ToolEntity::getStatus,
                ToolStatus.FAILED);
        long failedTools = toolRepository.selectCount(failedWrapper);
        statistics.setFailedTools(failedTools);

        // 官方工具数量
        LambdaQueryWrapper<ToolEntity> officialWrapper = Wrappers.<ToolEntity>lambdaQuery().eq(ToolEntity::getIsOffice,
                true);
        long officialTools = toolRepository.selectCount(officialWrapper);
        statistics.setOfficialTools(officialTools);

        return statistics;
    }

    /** 仅更新工具全局状态，不触发审核流程
     * 
     * @param toolId 工具ID
     * @param isGlobal 是否为全局工具 */
    public void updateToolGlobalStatus(String toolId, Boolean isGlobal) {
        // 1. 更新主工具表
        LambdaUpdateWrapper<ToolEntity> toolWrapper = Wrappers.<ToolEntity>lambdaUpdate()
                .eq(ToolEntity::getId, toolId)
                .set(ToolEntity::getIsGlobal, isGlobal);
        toolRepository.checkedUpdate(toolWrapper);

        // 2. 同步更新所有已安装的user_tools记录
        LambdaUpdateWrapper<UserToolEntity> userToolWrapper = Wrappers.<UserToolEntity>lambdaUpdate()
                .eq(UserToolEntity::getToolId, toolId)
                .set(UserToolEntity::getIsGlobal, isGlobal);
        userToolRepository.update(null, userToolWrapper);
    }

    /** 根据MCP服务器名称获取用户已安装的工具
     * 
     * @param serverName MCP服务器名称
     * @param userId 用户ID
     * @return 用户已安装的工具实体，如果不存在返回null */
    public UserToolEntity getUserInstalledToolByServerName(String serverName, String userId) {
        if (serverName == null || serverName.trim().isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<UserToolEntity> wrapper = Wrappers.<UserToolEntity>lambdaQuery()
                .eq(UserToolEntity::getMcpServerName, serverName)
                .eq(UserToolEntity::getUserId, userId);

        return userToolRepository.selectList(wrapper).get(0);
    }

    /** 根据MCP服务器名称获取用户已安装的工具对应的原始工具
     * 
     * @param serverName MCP服务器名称
     * @param userId 用户ID
     * @return 工具实体，如果不存在返回null */
    public ToolEntity getToolByServerNameForUsage(String serverName, String userId) {
        if (serverName == null || serverName.trim().isEmpty()) {
            return null;
        }

        // 查询用户是否安装了该工具
        UserToolEntity userTool = getUserInstalledToolByServerName(serverName, userId);
        if (userTool != null) {
            // 返回对应的原始工具信息
            return toolRepository.selectById(userTool.getToolId());
        }
        
        return null;
    }

    /** 校验用户MCP服务器名称唯一性
     * 
     * @param mcpServerName MCP服务器名称
     * @param userId 用户ID
     * @param excludeToolId 排除的工具ID（更新时使用）*/
    private void validateMcpServerNameUnique(String mcpServerName, String userId, String excludeToolId) {
        // 检查用户已安装工具中是否已存在此名称
        LambdaQueryWrapper<UserToolEntity> userToolWrapper = Wrappers.<UserToolEntity>lambdaQuery()
                .eq(UserToolEntity::getMcpServerName, mcpServerName)
                .eq(UserToolEntity::getUserId, userId);
        
        // 如果是工具更新，需要排除来自同一工具的安装记录
        if (excludeToolId != null) {
            userToolWrapper.ne(UserToolEntity::getToolId, excludeToolId);
        }
        
        long userToolCount = userToolRepository.selectCount(userToolWrapper);
        if (userToolCount > 0) {
            throw new BusinessException("MCP服务器名称 '" + mcpServerName + "' 与已安装工具冲突，请使用其他名称");
        }
    }
}