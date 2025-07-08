package org.xhy.domain.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.xhy.domain.agent.constant.PublishStatus;
import org.xhy.domain.agent.model.*;
import org.xhy.domain.agent.repository.AgentRepository;
import org.xhy.domain.agent.repository.AgentVersionRepository;
import org.xhy.domain.agent.repository.AgentWorkspaceRepository;
import org.xhy.domain.user.repository.UserRepository;
import org.xhy.domain.user.model.UserEntity;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.application.agent.dto.AgentWithUserDTO;
import org.xhy.application.agent.dto.AgentVersionDTO;
import org.xhy.application.agent.dto.AgentStatisticsDTO;
import org.xhy.application.agent.assembler.AgentVersionAssembler;
import org.xhy.interfaces.dto.agent.request.QueryAgentRequest;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Agent服务实现类 */
@Service
public class AgentDomainService {

    private final AgentRepository agentRepository;
    private final AgentVersionRepository agentVersionRepository;
    private final AgentWorkspaceRepository agentWorkspaceRepository;
    private final UserRepository userRepository;

    public AgentDomainService(AgentRepository agentRepository, AgentVersionRepository agentVersionRepository,
            AgentWorkspaceRepository agentWorkspaceRepository, UserRepository userRepository) {
        this.agentRepository = agentRepository;
        this.agentVersionRepository = agentVersionRepository;
        this.agentWorkspaceRepository = agentWorkspaceRepository;
        this.userRepository = userRepository;
    }

    /** 创建新Agent */
    public AgentEntity createAgent(AgentEntity agent) {
        agentRepository.insert(agent);
        return agent;
    }

    /** 获取单个Agent信息 */
    public AgentEntity getAgent(String agentId, String userId) {

        // 需要根据 agentId 和 userId 作为条件进行查询
        LambdaQueryWrapper<AgentEntity> wrapper = Wrappers.<AgentEntity>lambdaQuery().eq(AgentEntity::getId, agentId)
                .eq(AgentEntity::getUserId, userId);
        AgentEntity agent = agentRepository.selectOne(wrapper);
        if (agent == null) {
            throw new BusinessException("Agent不存在: " + agentId);
        }
        return agent;
    }

    /** 获取用户的Agent列表，支持状态和名称过滤 */
    public List<AgentEntity> getUserAgents(String userId, AgentEntity agent) {

        // 创建基础查询条件
        LambdaQueryWrapper<AgentEntity> queryWrapper = Wrappers.<AgentEntity>lambdaQuery()
                .eq(AgentEntity::getUserId, userId)
                .like(!StringUtils.isEmpty(agent.getName()), AgentEntity::getName, agent.getName())
                .orderByDesc(AgentEntity::getUpdatedAt);

        // 执行查询并返回结果
        return agentRepository.selectList(queryWrapper);
    }

    /** 获取已上架的Agent列表，支持名称搜索 当name为空时返回所有已上架Agent */
    public List<AgentVersionEntity> getPublishedAgentsByName(AgentEntity agent) {
        // 使用带名称和状态条件的查询
        List<AgentVersionEntity> latestVersions = agentVersionRepository
                .selectLatestVersionsByNameAndStatus(agent.getName(), PublishStatus.PUBLISHED.getCode());

        // 组合助理和版本信息
        return combineAgentsWithVersions(latestVersions);
    }

    /** 更新Agent信息（基本信息和配置合并更新） */
    public AgentEntity updateAgent(AgentEntity updateEntity) {

        // 需要根据 agentId 和 userId 作为条件进行修改
        LambdaUpdateWrapper<AgentEntity> wrapper = Wrappers.<AgentEntity>lambdaUpdate()
                .eq(AgentEntity::getId, updateEntity.getId()).eq(AgentEntity::getUserId, updateEntity.getUserId());
        agentRepository.checkedUpdate(updateEntity, wrapper);
        return updateEntity;
    }

    /** 切换Agent的启用/禁用状态 */
    public AgentEntity toggleAgentStatus(String agentId) {

        AgentEntity agent = agentRepository.selectById(agentId);
        if (agent == null) {
            throw new BusinessException("Agent不存在: " + agentId);
        }

        // 根据当前状态切换
        if (Boolean.TRUE.equals(agent.getEnabled())) {
            agent.disable();
        } else {
            agent.enable();
        }

        agentRepository.checkedUpdateById(agent);
        return agent;
    }

    /** 删除Agent */
    public void deleteAgent(String agentId, String userId) {
        // 根据agentId和userId删除即可，创建 wrapper
        LambdaQueryWrapper<AgentEntity> wrapper = Wrappers.<AgentEntity>lambdaQuery().eq(AgentEntity::getId, agentId)
                .eq(AgentEntity::getUserId, userId);
        agentRepository.checkedDelete(wrapper);
        // 删除版本
        agentVersionRepository.delete(Wrappers.<AgentVersionEntity>lambdaQuery()
                .eq(AgentVersionEntity::getAgentId, agentId).eq(AgentVersionEntity::getUserId, userId));
    }

    /** 发布Agent版本 */
    public AgentVersionEntity publishAgentVersion(String agentId, AgentVersionEntity versionEntity) {
        AgentEntity agent = agentRepository.selectById(agentId);
        if (agent == null) {
            throw new BusinessException("Agent不存在: " + agentId);
        }

        // 查询最新版本号进行比较
        LambdaQueryWrapper<AgentVersionEntity> latestVersionQuery = Wrappers.<AgentVersionEntity>lambdaQuery()
                .eq(AgentVersionEntity::getAgentId, agentId)
                .eq(AgentVersionEntity::getUserId, versionEntity.getUserId())
                .orderByDesc(AgentVersionEntity::getPublishedAt).last("LIMIT 1");

        AgentVersionEntity latestVersion = agentVersionRepository.selectOne(latestVersionQuery);

        if (latestVersion != null) {
            // 版本号比较
            String newVersion = versionEntity.getVersionNumber();
            String oldVersion = latestVersion.getVersionNumber();

            // 检查是否为相同版本号
            if (newVersion.equals(oldVersion)) {
                throw new BusinessException("版本号已存在: " + newVersion);
            }

            // 检查新版本号是否大于旧版本号
            if (!isVersionGreaterThan(newVersion, oldVersion)) {
                throw new BusinessException("新版本号(" + newVersion + ")必须大于当前最新版本号(" + oldVersion + ")");
            }
        }

        // 设置版本关联的Agent ID
        versionEntity.setAgentId(agentId);

        // 设置版本状态为审核中
        versionEntity.setPublishStatus(PublishStatus.REVIEWING.getCode());

        // 保存版本
        agentVersionRepository.insert(versionEntity);

        return versionEntity;
    }

    /** 更新版本发布状态 */
    public AgentVersionEntity updateVersionPublishStatus(String versionId, PublishStatus status) {
        AgentVersionEntity version = agentVersionRepository.selectById(versionId);
        if (version == null) {
            throw new BusinessException("版本不存在: " + versionId);
        }

        version.setRejectReason("");

        // 更新版本状态
        version.updatePublishStatus(status);
        agentVersionRepository.updateById(version);

        // 如果状态更新为已发布，则绑定为Agent的publishedVersion
        if (status == PublishStatus.PUBLISHED) {
            AgentEntity agent = agentRepository.selectById(version.getAgentId());
            if (agent != null) {
                agent.publishVersion(versionId);
                agentRepository.checkedUpdateById(agent);
            }
        }

        return version;
    }

    /** 拒绝版本发布 */
    public AgentVersionEntity rejectVersion(String versionId, String reason) {
        AgentVersionEntity version = agentVersionRepository.selectById(versionId);
        if (version == null) {
            throw new BusinessException("版本不存在: " + versionId);
        }

        // 拒绝版本发布
        version.reject(reason);
        agentVersionRepository.checkedUpdateById(version);

        return version;
    }

    /** 获取Agent的所有版本 */
    public List<AgentVersionEntity> getAgentVersions(String agentId, String userId) {
        // 查询Agent
        AgentEntity agent = agentRepository.selectById(agentId);
        if (agent == null) {
            throw new BusinessException("Agent不存在");
        }

        // 如果userId不为空，需要检查权限（普通用户只能访问自己的Agent版本）
        // 如果userId为空，表示管理员访问，跳过权限检查
        if (userId != null && !agent.getUserId().equals(userId)) {
            throw new BusinessException("Agent不存在或无权访问");
        }

        // 查询所有版本并按创建时间降序排序
        LambdaQueryWrapper<AgentVersionEntity> wrapper = Wrappers.<AgentVersionEntity>lambdaQuery()
                .eq(AgentVersionEntity::getAgentId, agentId).orderByDesc(AgentVersionEntity::getCreatedAt);
        return agentVersionRepository.selectList(wrapper);
    }

    /** 获取Agent的特定版本 */
    public AgentVersionEntity getAgentVersion(String agentId, String versionNumber) {
        // 使用agentId和versionNumber查询版本
        LambdaQueryWrapper<AgentVersionEntity> wrapper = Wrappers.<AgentVersionEntity>lambdaQuery()
                .eq(AgentVersionEntity::getAgentId, agentId).eq(AgentVersionEntity::getVersionNumber, versionNumber);
        AgentVersionEntity version = agentVersionRepository.selectOne(wrapper);
        if (version == null) {
            throw new BusinessException("Agent版本不存在: " + versionNumber);
        }
        return version;
    }

    /** 获取Agent的最新版本 */
    public AgentVersionEntity getLatestAgentVersion(String agentId) {
        LambdaQueryWrapper<AgentVersionEntity> queryWrapper = Wrappers.<AgentVersionEntity>lambdaQuery()
                .eq(AgentVersionEntity::getAgentId, agentId).orderByDesc(AgentVersionEntity::getPublishedAt)
                .last("LIMIT 1");

        AgentVersionEntity version = agentVersionRepository.selectOne(queryWrapper);
        if (version == null) {
            return null; // 第一次发布时没有版本，返回null而不是抛出异常
        }
        return version;
    }

    /** 获取指定状态的所有版本 注：只返回每个助理的最新版本，避免同一助理多个版本同时出现 */
    public List<AgentVersionEntity> getVersionsByStatus(PublishStatus status) {

        // 直接通过SQL查询每个agentId的最新版本
        return agentVersionRepository.selectLatestVersionsByStatus(status == null ? null : status.getCode());
    }

    /** 校验 agent 是否存在 */
    public boolean exist(String agentId, String userId) {

        LambdaQueryWrapper<AgentEntity> wrapper = Wrappers.<AgentEntity>lambdaQuery().eq(AgentEntity::getId, agentId)
                .eq(AgentEntity::getUserId, userId);
        AgentEntity agent = agentRepository.selectOne(wrapper);
        return agent != null;
    }

    /** 根据 agentIds 获取 agents /** 根据 agentIds 获取 agents */
    public List<AgentEntity> getAgentsByIds(List<String> agentIds) {
        return agentRepository.selectByIds(agentIds);
    }

    public AgentEntity getAgentById(String agentId) {
        return this.getAgentsByIds(Collections.singletonList(agentId)).get(0);
    }

    public AgentEntity getAgentWithPermissionCheck(String agentId, String userId) {

        // 检查工作区是否存在
        boolean b1 = agentWorkspaceRepository.exist(agentId, userId);

        boolean b2 = exist(agentId, userId);
        if (!b1 && !b2) {
            throw new BusinessException("助理不存在");
        }
        AgentEntity agentEntity = getAgentById(agentId);

        // 如果有版本则使用版本
        String publishedVersion = agentEntity.getPublishedVersion();
        if (!StringUtils.isEmpty(publishedVersion)) {
            AgentVersionEntity agentVersionEntity = getAgentVersionById(publishedVersion);
            BeanUtils.copyProperties(agentVersionEntity, agentEntity);
        }

        return agentEntity;
    }

    public AgentVersionEntity getAgentVersionById(String versionId) {
        return agentVersionRepository.selectById(versionId);
    }

    /** 组合助理和版本信息
     *
     * @param versionEntities 版本实体列表
     * @return 组合后的版本AgentVersionEntity列表 */
    private List<AgentVersionEntity> combineAgentsWithVersions(List<AgentVersionEntity> versionEntities) {
        // 如果版本列表为空，直接返回空列表
        if (versionEntities == null || versionEntities.isEmpty()) {
            return Collections.emptyList();
        }

        // 根据版本中的 agent_id 以及 enable == true 查出对应的 agents
        List<AgentEntity> agents = agentRepository.selectList(Wrappers.<AgentEntity>lambdaQuery()
                .in(AgentEntity::getId,
                        versionEntities.stream().map(AgentVersionEntity::getAgentId).collect(Collectors.toList()))
                .eq(AgentEntity::getEnabled, true));

        // 将版本转为 map，key：agent_id，value：本身
        Map<String, AgentVersionEntity> agentVersionMap = versionEntities.stream()
                .collect(Collectors.toMap(AgentVersionEntity::getAgentId, Function.identity()));

        return agents.stream().map(agent -> agentVersionMap.get(agent.getId())).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /** 比较版本号大小
     *
     * @param newVersion 新版本号
     * @param oldVersion 旧版本号
     * @return 如果新版本大于旧版本返回true，否则返回false */
    private boolean isVersionGreaterThan(String newVersion, String oldVersion) {
        if (oldVersion == null || oldVersion.trim().isEmpty()) {
            return true; // 如果没有旧版本，新版本肯定更大
        }

        // 分割版本号
        String[] current = newVersion.split("\\.");
        String[] last = oldVersion.split("\\.");

        // 确保版本号格式正确
        if (current.length != 3 || last.length != 3) {
            throw new BusinessException("版本号必须遵循 x.y.z 格式");
        }

        try {
            // 比较主版本号
            int currentMajor = Integer.parseInt(current[0]);
            int lastMajor = Integer.parseInt(last[0]);
            if (currentMajor > lastMajor)
                return true;
            if (currentMajor < lastMajor)
                return false;

            // 主版本号相同，比较次版本号
            int currentMinor = Integer.parseInt(current[1]);
            int lastMinor = Integer.parseInt(last[1]);
            if (currentMinor > lastMinor)
                return true;
            if (currentMinor < lastMinor)
                return false;

            // 主版本号和次版本号都相同，比较修订版本号
            int currentPatch = Integer.parseInt(current[2]);
            int lastPatch = Integer.parseInt(last[2]);

            return currentPatch > lastPatch;
        } catch (NumberFormatException e) {
            throw new BusinessException("版本号格式错误，必须是数字: " + e.getMessage());
        }
    }

    /** 分页查询Agent列表
     * 
     * @param queryAgentRequest 查询条件
     * @return Agent分页数据 */
    public Page<AgentEntity> getAgents(QueryAgentRequest queryAgentRequest) {
        LambdaQueryWrapper<AgentEntity> wrapper = Wrappers.<AgentEntity>lambdaQuery();

        // 关键词搜索：Agent名称、描述
        if (queryAgentRequest.getKeyword() != null && !queryAgentRequest.getKeyword().trim().isEmpty()) {
            String keyword = queryAgentRequest.getKeyword().trim();
            wrapper.and(w -> w.like(AgentEntity::getName, keyword).or().like(AgentEntity::getDescription, keyword));
        }

        // 状态筛选
        if (queryAgentRequest.getEnabled() != null) {
            wrapper.eq(AgentEntity::getEnabled, queryAgentRequest.getEnabled());
        }

        // 按创建时间倒序排列
        wrapper.orderByDesc(AgentEntity::getCreatedAt);

        // 分页查询
        long current = queryAgentRequest.getPage() != null ? queryAgentRequest.getPage().longValue() : 1L;
        long size = queryAgentRequest.getPageSize() != null ? queryAgentRequest.getPageSize().longValue() : 15L;
        Page<AgentEntity> page = new Page<>(current, size);
        return agentRepository.selectPage(page, wrapper);
    }

    /** 获取带用户信息的Agent分页数据
     * 
     * @param agentPage Agent分页数据
     * @return 包含用户信息的Agent分页数据 */
    public Page<AgentWithUserDTO> getAgentsWithUserInfo(Page<AgentEntity> agentPage) {
        if (agentPage.getRecords().isEmpty()) {
            Page<AgentWithUserDTO> result = new Page<>();
            result.setCurrent(agentPage.getCurrent());
            result.setSize(agentPage.getSize());
            result.setTotal(agentPage.getTotal());
            result.setRecords(new ArrayList<>());
            return result;
        }

        // 获取所有用户ID
        List<String> userIds = agentPage.getRecords().stream().map(AgentEntity::getUserId).distinct()
                .collect(Collectors.toList());

        // 批量查询用户信息
        List<UserEntity> users = userRepository.selectBatchIds(userIds);
        Map<String, UserEntity> userMap = users.stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        // 获取所有Agent的版本信息
        List<String> agentIds = agentPage.getRecords().stream().map(AgentEntity::getId).collect(Collectors.toList());
        Map<String, List<AgentVersionEntity>> versionMap = getVersionsForAgents(agentIds);

        // 组装结果
        List<AgentWithUserDTO> records = agentPage.getRecords().stream().map(agent -> convertToAgentWithUserDTO(agent,
                userMap.get(agent.getUserId()), versionMap.get(agent.getId()))).collect(Collectors.toList());

        Page<AgentWithUserDTO> result = new Page<>();
        result.setCurrent(agentPage.getCurrent());
        result.setSize(agentPage.getSize());
        result.setTotal(agentPage.getTotal());
        result.setRecords(records);
        return result;
    }

    /** 批量获取Agent的版本信息 */
    private Map<String, List<AgentVersionEntity>> getVersionsForAgents(List<String> agentIds) {
        if (agentIds.isEmpty()) {
            return new HashMap<>();
        }

        LambdaQueryWrapper<AgentVersionEntity> wrapper = Wrappers.<AgentVersionEntity>lambdaQuery()
                .in(AgentVersionEntity::getAgentId, agentIds).orderByDesc(AgentVersionEntity::getCreatedAt);

        List<AgentVersionEntity> allVersions = agentVersionRepository.selectList(wrapper);

        return allVersions.stream().collect(Collectors.groupingBy(AgentVersionEntity::getAgentId));
    }

    /** 转换AgentEntity为AgentWithUserDTO */
    private AgentWithUserDTO convertToAgentWithUserDTO(AgentEntity agent, UserEntity user,
            List<AgentVersionEntity> versions) {
        AgentWithUserDTO dto = new AgentWithUserDTO();
        dto.setId(agent.getId());
        dto.setName(agent.getName());
        dto.setAvatar(agent.getAvatar());
        dto.setDescription(agent.getDescription());
        dto.setSystemPrompt(agent.getSystemPrompt());
        dto.setWelcomeMessage(agent.getWelcomeMessage());
        dto.setToolIds(agent.getToolIds());
        dto.setKnowledgeBaseIds(agent.getKnowledgeBaseIds());
        dto.setPublishedVersion(agent.getPublishedVersion());
        dto.setEnabled(agent.getEnabled());
        dto.setUserId(agent.getUserId());
        dto.setToolPresetParams(agent.getToolPresetParams());
        dto.setCreatedAt(agent.getCreatedAt());
        dto.setUpdatedAt(agent.getUpdatedAt());
        dto.setMultiModal(agent.getMultiModal());

        // 设置用户信息
        if (user != null) {
            dto.setUserNickname(user.getNickname());
            dto.setUserEmail(user.getEmail());
            dto.setUserAvatarUrl(user.getAvatarUrl());
        }

        // 设置版本信息
        if (versions != null && !versions.isEmpty()) {
            List<AgentVersionDTO> versionDTOs = AgentVersionAssembler.toDTOs(versions);
            dto.setVersions(versionDTOs);
        }

        return dto;
    }

    /** 获取Agent统计信息 */
    public AgentStatisticsDTO getAgentStatistics() {
        AgentStatisticsDTO statistics = new AgentStatisticsDTO();

        // 总Agent数量
        long totalAgents = agentRepository.selectCount(null);
        statistics.setTotalAgents(totalAgents);

        // 启用的Agent数量
        LambdaQueryWrapper<AgentEntity> enabledWrapper = Wrappers.<AgentEntity>lambdaQuery().eq(AgentEntity::getEnabled,
                true);
        long enabledAgents = agentRepository.selectCount(enabledWrapper);
        statistics.setEnabledAgents(enabledAgents);

        // 禁用的Agent数量
        LambdaQueryWrapper<AgentEntity> disabledWrapper = Wrappers.<AgentEntity>lambdaQuery()
                .eq(AgentEntity::getEnabled, false);
        long disabledAgents = agentRepository.selectCount(disabledWrapper);
        statistics.setDisabledAgents(disabledAgents);

        // 待审核版本数量（状态为REVIEWING的版本）
        LambdaQueryWrapper<AgentVersionEntity> pendingWrapper = Wrappers.<AgentVersionEntity>lambdaQuery()
                .eq(AgentVersionEntity::getPublishStatus, PublishStatus.REVIEWING.getCode());
        long pendingVersions = agentVersionRepository.selectCount(pendingWrapper);
        statistics.setPendingVersions(pendingVersions);

        return statistics;
    }
}