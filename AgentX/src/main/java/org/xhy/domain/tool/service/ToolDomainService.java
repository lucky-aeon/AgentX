package org.xhy.domain.tool.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
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
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** 工具领域服务 */
@Service
public class ToolDomainService {

    /** 技术验证阶段状态 */
    private static final Set<ToolStatus> TECHNICAL_VALIDATION_STATUSES = Set.of(ToolStatus.WAITING_REVIEW,
            ToolStatus.GITHUB_URL_VALIDATE, ToolStatus.DEPLOYING, ToolStatus.FETCHING_TOOLS);

    /** 审核流程中状态 */
    private static final Set<ToolStatus> IN_REVIEW_PROCESS_STATUSES = Set.of(ToolStatus.WAITING_REVIEW,
            ToolStatus.GITHUB_URL_VALIDATE, ToolStatus.DEPLOYING, ToolStatus.FETCHING_TOOLS, ToolStatus.MANUAL_REVIEW);

    /** 终态状态 */
    private static final Set<ToolStatus> FINAL_STATUSES = Set.of(ToolStatus.APPROVED, ToolStatus.FAILED);

    private final ToolRepository toolRepository;
    private final ToolVersionRepository toolVersionRepository;
    private final ToolStateDomainService toolStateService;
    private final UserToolRepository userToolRepository;
    private final UserRepository userRepository;

    public ToolDomainService(ToolRepository toolRepository, ToolVersionRepository toolVersionRepository,
            ToolStateDomainService toolStateService, UserToolRepository userToolRepository,
            UserRepository userRepository) {
        this.toolRepository = toolRepository;
        this.toolVersionRepository = toolVersionRepository;
        this.toolStateService = toolStateService;
        this.userToolRepository = userToolRepository;
        this.userRepository = userRepository;
    }

    /** 创建工具
     *
     * @param toolEntity 工具实体
     * @return 创建后的工具实体 */
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

        LambdaUpdateWrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaUpdate().eq(ToolEntity::getId, toolId)
                .set(ToolEntity::getStatus, status);
        toolRepository.checkedUpdate(wrapper);
        return toolRepository.selectById(toolId);
    }

    public ToolEntity updateTool(ToolEntity toolEntity) {
        // 1. 获取原工具信息并验证
        ToolEntity oldTool = toolRepository.selectById(toolEntity.getId());
        validateToolExists(oldTool);

        // 2. 检查是否在审核流程中
        if (IN_REVIEW_PROCESS_STATUSES.contains(oldTool.getStatus())) {
            throw new BusinessException("工具审核中，不允许修改");
        }

        // 3. 判断修改类型
        boolean modifiedTechnicalFields = hasTechnicalFieldsChanged(toolEntity, oldTool);
        boolean modifiedBasicFields = hasBasicFieldsChanged(toolEntity, oldTool);

        if (!modifiedTechnicalFields && !modifiedBasicFields) {
            throw new BusinessException("未检测到任何修改");
        }

        // 4. 根据修改类型设置状态
        if (modifiedTechnicalFields) {
            // 修改了技术字段：重新完整审核
            toolEntity.setStatus(ToolStatus.WAITING_REVIEW);
            toolEntity.setFailedStepStatus(null);
            toolEntity.setRejectReason(null);
            String mcpServerName = getMcpServerName(toolEntity);
            toolEntity.setMcpServerName(mcpServerName);
        } else {
            // 只修改了基本信息
            handleBasicFieldsModification(toolEntity, oldTool);
        }

        // 5. 更新数据库
        LambdaUpdateWrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaUpdate()
                .eq(ToolEntity::getId, toolEntity.getId())
                .eq(toolEntity.needCheckUserId(), ToolEntity::getUserId, toolEntity.getUserId());
        toolRepository.update(toolEntity, wrapper);

        // 6. 如果需要状态流转，提交处理
        if (shouldSubmitForProcessing(toolEntity.getStatus())) {
            toolStateService.submitToolForProcessing(toolEntity);
        }

        return toolEntity;
    }

    @Transactional
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

    /** 检查是否修改了关键技术字段
     * 
     * @param newTool 新工具信息
     * @param oldTool 原工具信息
     * @return 是否修改了技术字段 */
    private boolean hasTechnicalFieldsChanged(ToolEntity newTool, ToolEntity oldTool) {
        return Stream
                .<Supplier<Boolean>>of(() -> !Objects.equals(newTool.getUploadUrl(), oldTool.getUploadUrl()),
                        () -> !Objects.equals(newTool.getInstallCommand(), oldTool.getInstallCommand()),
                        () -> !Objects.equals(newTool.getToolType(), oldTool.getToolType()),
                        () -> !Objects.equals(newTool.getUploadType(), oldTool.getUploadType()))
                .anyMatch(Supplier::get);
    }

    /** 检查是否修改了基本信息字段
     * 
     * @param newTool 新工具信息
     * @param oldTool 原工具信息
     * @return 是否修改了基本信息字段 */
    private boolean hasBasicFieldsChanged(ToolEntity newTool, ToolEntity oldTool) {
        return Stream.<Supplier<Boolean>>of(() -> !Objects.equals(newTool.getName(), oldTool.getName()),
                () -> !Objects.equals(newTool.getDescription(), oldTool.getDescription()),
                () -> !Objects.equals(newTool.getIcon(), oldTool.getIcon()),
                () -> !Objects.equals(newTool.getSubtitle(), oldTool.getSubtitle()),
                () -> !Objects.equals(newTool.getLabels(), oldTool.getLabels())).anyMatch(Supplier::get);
    }

    /** 处理基本信息字段修改的状态设置
     * 
     * @param toolEntity 要更新的工具实体
     * @param oldTool 原工具信息 */
    private void handleBasicFieldsModification(ToolEntity toolEntity, ToolEntity oldTool) {
        if (oldTool.getStatus() == ToolStatus.APPROVED) {
            // 已通过的工具修改基本信息：只需人工审核
            toolEntity.setStatus(ToolStatus.MANUAL_REVIEW);
        } else if (oldTool.getStatus() == ToolStatus.FAILED) {
            if (TECHNICAL_VALIDATION_STATUSES.contains(oldTool.getFailedStepStatus())) {
                // 技术验证失败：从失败步骤重新开始
                toolEntity.setStatus(oldTool.getFailedStepStatus());
                // 保留失败信息
                toolEntity.setFailedStepStatus(oldTool.getFailedStepStatus());
                toolEntity.setRejectReason(oldTool.getRejectReason());
            } else {
                // 人工审核失败：可以直接人工审核
                toolEntity.setStatus(ToolStatus.MANUAL_REVIEW);
                toolEntity.setFailedStepStatus(null);
                toolEntity.setRejectReason(null);
            }
        }
    }

    /** 判断是否需要提交状态流转处理
     * 
     * @param status 当前状态
     * @return 是否需要提交处理 */
    private boolean shouldSubmitForProcessing(ToolStatus status) {
        return status == ToolStatus.WAITING_REVIEW || TECHNICAL_VALIDATION_STATUSES.contains(status);
    }

    /** 验证工具是否存在
     * 
     * @param tool 工具实体
     * @throws BusinessException 工具不存在时抛出 */
    private void validateToolExists(ToolEntity tool) {
        if (tool == null) {
            throw new BusinessException("工具不存在");
        }
    }
}