package org.xhy.domain.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.domain.agent.constant.PublishStatus;
import org.xhy.domain.agent.model.*;
import org.xhy.domain.agent.repository.AgentRepository;
import org.xhy.domain.agent.repository.AgentVersionRepository;
import org.xhy.domain.agent.repository.AgentWorkspaceRepository;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Agent服务实现类
 */
@Service
public class AgentDomainService {

    private final AgentRepository agentRepository;
    private final AgentVersionRepository agentVersionRepository;
    private final AgentWorkspaceRepository agentWorkspaceRepository;

    public AgentDomainService(AgentRepository agentRepository, AgentVersionRepository agentVersionRepository, AgentWorkspaceRepository agentWorkspaceRepository) {
        this.agentRepository = agentRepository;
        this.agentVersionRepository = agentVersionRepository;
        this.agentWorkspaceRepository = agentWorkspaceRepository;
    }

    /**
     * 创建新Agent
     */
    @Transactional
    public AgentEntity createAgent(AgentEntity agent) {
        agentRepository.insert(agent);
        return agent;
    }

    /**
     * 获取单个Agent信息
     */
    public AgentEntity getAgent(String agentId, String userId) {

        // 需要根据 agentId 和 userId 作为条件进行查询
        LambdaQueryWrapper<AgentEntity> wrapper = Wrappers.<AgentEntity>lambdaQuery()
                .eq(AgentEntity::getId, agentId)
                .eq(AgentEntity::getUserId, userId);
        AgentEntity agent = agentRepository.selectOne(wrapper);
        if (agent == null) {
            throw new BusinessException("Agent不存在: " + agentId);
        }
        return agent;
    }

    /**
     * 获取用户的Agent列表，支持状态和名称过滤
     */
    public List<AgentEntity> getUserAgents(String userId, AgentEntity agent) {

        // 创建基础查询条件
        LambdaQueryWrapper<AgentEntity> queryWrapper = Wrappers.<AgentEntity>lambdaQuery()
                .eq(AgentEntity::getUserId, userId)
                .like(!StringUtils.isEmpty(agent.getName()), AgentEntity::getName,
                        agent.getName())
                .orderByDesc(AgentEntity::getUpdatedAt);

        // 执行查询并返回结果
        return agentRepository.selectList(queryWrapper);
    }

    /**
     * 获取已上架的Agent列表，支持名称搜索
     * 当name为空时返回所有已上架Agent
     */
    public List<AgentVersionEntity> getPublishedAgentsByName(AgentEntity agent) {
        // 使用带名称和状态条件的查询
        List<AgentVersionEntity> latestVersions = agentVersionRepository.selectLatestVersionsByNameAndStatus(
                agent.getName(),
                PublishStatus.PUBLISHED.getCode());

        // 组合助理和版本信息
        return combineAgentsWithVersions(latestVersions);
    }

    /**
     * 更新Agent信息（基本信息和配置合并更新）
     */
    @Transactional
    public AgentEntity updateAgent(String agentId, AgentEntity updateEntity) {

        // 需要根据 agentId 和 userId 作为条件进行修改
        LambdaUpdateWrapper<AgentEntity> wrapper = Wrappers.<AgentEntity>lambdaUpdate()
                .eq(AgentEntity::getId, agentId)
                .eq(AgentEntity::getUserId, updateEntity.getUserId());
        agentRepository.update(updateEntity, wrapper);
        return updateEntity;
    }

    /**
     * 切换Agent的启用/禁用状态
     */
    @Transactional
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

        agentRepository.updateById(agent);
        return agent;
    }

    /**
     * 删除Agent
     */
    @Transactional
    public void deleteAgent(String agentId, String userId) {
        // 根据agentId和userId删除即可，创建 wrapper
        LambdaQueryWrapper<AgentEntity> wrapper = Wrappers.<AgentEntity>lambdaQuery()
                .eq(AgentEntity::getId, agentId)
                .eq(AgentEntity::getUserId, userId);
        agentRepository.delete(wrapper);
        // 删除版本
        agentVersionRepository.delete(Wrappers.<AgentVersionEntity>lambdaQuery()
                .eq(AgentVersionEntity::getAgentId, agentId)
                .eq(AgentVersionEntity::getUserId, userId));
    }

    /**
     * 发布Agent版本
     */
    @Transactional
    public AgentVersionEntity publishAgentVersion(String agentId, AgentVersionEntity versionEntity) {
        AgentEntity agent = agentRepository.selectById(agentId);
        if (agent == null) {
            throw new BusinessException("Agent不存在: " + agentId);
        }

        // 查询最新版本号进行比较
        LambdaQueryWrapper<AgentVersionEntity> latestVersionQuery = Wrappers.<AgentVersionEntity>lambdaQuery()
                .eq(AgentVersionEntity::getAgentId, agentId)
                .eq(AgentVersionEntity::getUserId, versionEntity.getUserId())
                .orderByDesc(AgentVersionEntity::getPublishedAt)
                .last("LIMIT 1");

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

    /**
     * 更新版本发布状态
     */
    @Transactional
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
                agentRepository.updateById(agent);
            }
        }

        return version;
    }

    /**
     * 拒绝版本发布
     */
    @Transactional
    public AgentVersionEntity rejectVersion(String versionId, String reason) {
        AgentVersionEntity version = agentVersionRepository.selectById(versionId);
        if (version == null) {
            throw new BusinessException("版本不存在: " + versionId);
        }

        // 拒绝版本发布
        version.reject(reason);
        agentVersionRepository.updateById(version);

        return version;
    }

    /**
     * 获取Agent的所有版本
     */
    public List<AgentVersionEntity> getAgentVersions(String agentId, String userId) {
        // 查询Agent
        AgentEntity agent = agentRepository.selectById(agentId);
        if (agent == null || !agent.getUserId().equals(userId)) {
            throw new BusinessException("Agent不存在或无权访问");
        }

        // 查询所有版本并按创建时间降序排序
        LambdaQueryWrapper<AgentVersionEntity> wrapper = Wrappers.<AgentVersionEntity>lambdaQuery()
                .eq(AgentVersionEntity::getAgentId, agentId)
                .orderByDesc(AgentVersionEntity::getCreatedAt);
        return agentVersionRepository.selectList(wrapper);
    }

    /**
     * 获取Agent的特定版本
     */
    public AgentVersionEntity getAgentVersion(String agentId, String versionNumber) {
        // 使用agentId和versionNumber查询版本
        LambdaQueryWrapper<AgentVersionEntity> wrapper = Wrappers.<AgentVersionEntity>lambdaQuery()
                .eq(AgentVersionEntity::getAgentId, agentId)
                .eq(AgentVersionEntity::getVersionNumber, versionNumber);
        AgentVersionEntity version = agentVersionRepository.selectOne(wrapper);
        if (version == null) {
            throw new BusinessException("Agent版本不存在: " + versionNumber);
        }
        return version;
    }

    /**
     * 获取Agent的最新版本
     */
    public AgentVersionEntity getLatestAgentVersion(String agentId) {
        LambdaQueryWrapper<AgentVersionEntity> queryWrapper = Wrappers.<AgentVersionEntity>lambdaQuery()
                .eq(AgentVersionEntity::getAgentId, agentId)
                .orderByDesc(AgentVersionEntity::getPublishedAt)
                .last("LIMIT 1");

        AgentVersionEntity version = agentVersionRepository.selectOne(queryWrapper);
        if (version == null) {
            return null; // 第一次发布时没有版本，返回null而不是抛出异常
        }
        return version;
    }

    /**
     * 获取指定状态的所有版本
     * 注：只返回每个助理的最新版本，避免同一助理多个版本同时出现
     */
    public List<AgentVersionEntity> getVersionsByStatus(PublishStatus status) {

        // 直接通过SQL查询每个agentId的最新版本
        return agentVersionRepository
                .selectLatestVersionsByStatus(status == null ? null : status.getCode());
    }

    /**
     * 校验 agent 是否存在
     */
    public boolean checkAgentExist(String agentId, String userId) {

        LambdaQueryWrapper<AgentEntity> wrapper = Wrappers.<AgentEntity>lambdaQuery()
                .eq(AgentEntity::getId, agentId)
                .eq(AgentEntity::getUserId, userId);
        AgentEntity agent = agentRepository.selectOne(wrapper);
        return agent !=null;
    }

    /**
     * 根据 agentIds 获取 agents
     * /**
     * 根据 agentIds 获取 agents
     */
    public List<AgentEntity> getAgentsByIds(List<String> agentIds) {
        return agentRepository.selectByIds(agentIds);
    }

    public AgentEntity getAgentById(String agentId) {
       return this.getAgentsByIds(Collections.singletonList(agentId)).get(0);
    }

    public AgentEntity getAgentWithPermissionCheck(String agentId, String userId) {
        
        // 检查工作区是否存在
        boolean b1 = agentWorkspaceRepository.checkAgentWorkspaceExist(agentId, userId);

        boolean b2 = checkAgentExist(agentId, userId);
        if (!b1 && !b2){
            throw new BusinessException("助理不存在");
        }
        AgentEntity agentEntity = getAgentById(agentId);

        // 如果有版本则使用版本
        String publishedVersion = agentEntity.getPublishedVersion();
        if (!StringUtils.isEmpty(publishedVersion)) {
            AgentVersionEntity agentVersionEntity = getAgentVersionById(publishedVersion);
            BeanUtils.copyProperties(agentVersionEntity,agentEntity);
        }

        return agentEntity;
    }

    public AgentVersionEntity getAgentVersionById(String versionId){
        return agentVersionRepository.selectById(versionId);
    }

    /**
     * 组合助理和版本信息
     *
     * @param versionEntities 版本实体列表
     * @return 组合后的版本AgentVersionEntity列表
     */
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

        return agents.stream()
                .map(agent -> agentVersionMap.get(agent.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 比较版本号大小
     *
     * @param newVersion 新版本号
     * @param oldVersion 旧版本号
     * @return 如果新版本大于旧版本返回true，否则返回false
     */
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
}