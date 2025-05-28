package org.xhy.domain.tool.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.xhy.domain.tool.model.ToolVersionEntity;
import org.xhy.domain.tool.repository.ToolVersionRepository;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.interfaces.dto.tool.request.QueryToolRequest;

@Service
public class ToolVersionDomainService {

    private final ToolVersionRepository toolVersionRepository;

    public ToolVersionDomainService(ToolVersionRepository toolVersionRepository) {
        this.toolVersionRepository = toolVersionRepository;
    }

    public Page<ToolVersionEntity> listToolVersion(QueryToolRequest queryToolRequest) {
        long page = queryToolRequest.getPage();
        long pageSize = queryToolRequest.getPageSize();
        String toolName = queryToolRequest.getToolName();
        // 1. 查询所有公开版本，支持名称模糊
        LambdaQueryWrapper<ToolVersionEntity> wrapper = Wrappers.<ToolVersionEntity>lambdaQuery()
                .eq(ToolVersionEntity::getPublicStatus, true)
                .like(toolName != null && !toolName.isEmpty(), ToolVersionEntity::getName, toolName)
                .orderByDesc(ToolVersionEntity::getCreatedAt);
        List<ToolVersionEntity> allPublicList = toolVersionRepository.selectList(wrapper);
        // 2. 按tool_id分组，取每组created_at最大的一条
        Map<String, ToolVersionEntity> latestMap = allPublicList.stream()
                .collect(java.util.stream.Collectors.toMap(ToolVersionEntity::getToolId, v -> v,
                        (v1, v2) -> v1.getCreatedAt().isAfter(v2.getCreatedAt()) ? v1 : v2));
        List<ToolVersionEntity> latestList = new java.util.ArrayList<>(latestMap.values());
        // 3. 按创建时间倒序排列
        latestList.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        // 4. 手动分页
        int fromIndex = (int) ((page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + (int) pageSize, latestList.size());
        List<ToolVersionEntity> pageList = fromIndex >= latestList.size()
                ? new java.util.ArrayList<>()
                : latestList.subList(fromIndex, toIndex);
        Page<ToolVersionEntity> resultPage = new Page<>(page, pageSize, latestList.size());
        resultPage.setRecords(pageList);
        return resultPage;
    }

    public ToolVersionEntity getToolVersion(String toolId, String version) {
        Wrapper<ToolVersionEntity> wrapper = Wrappers.<ToolVersionEntity>lambdaQuery()
                .eq(ToolVersionEntity::getToolId, toolId).eq(ToolVersionEntity::getVersion, version);
        ToolVersionEntity toolVersionEntity = toolVersionRepository.selectOne(wrapper);
        if (toolVersionEntity == null) {
            throw new BusinessException("工具版本不存在: " + toolId + " " + version);
        }
        return toolVersionEntity;
    }

    public void addToolVersion(ToolVersionEntity toolVersionEntity) {
        toolVersionRepository.insert(toolVersionEntity);
    }

    public ToolVersionEntity findLatestToolVersion(String toolId, String userId) {

        LambdaQueryWrapper<ToolVersionEntity> queryWrapper = Wrappers.<ToolVersionEntity>lambdaQuery()
                .eq(ToolVersionEntity::getToolId, toolId).orderByDesc(ToolVersionEntity::getCreatedAt).last("LIMIT 1");

        ToolVersionEntity toolVersionEntity = toolVersionRepository.selectOne(queryWrapper);
        if (toolVersionEntity == null) {
            return null; // 第一次发布时没有版本，返回null而不是抛出异常
        }
        return toolVersionEntity;
    }

    /** 获取工具的所有版本，各根据当前用户判断，如果是当前用户则返回所有版本，如果不是则返回公开的版本
     * @param toolId 工具 id
     * @param userId 用户 id
     * @return */
    public List<ToolVersionEntity> getToolVersions(String toolId, String userId) {
        // 先查询工具的创建者是谁
        LambdaQueryWrapper<ToolVersionEntity> creatorQuery = Wrappers.<ToolVersionEntity>lambdaQuery()
                .eq(ToolVersionEntity::getToolId, toolId).orderByDesc(ToolVersionEntity::getCreatedAt).last("LIMIT 1");
        ToolVersionEntity tool = toolVersionRepository.selectOne(creatorQuery);

        // 如果工具不存在，返回空列表
        if (tool == null) {
            throw new BusinessException("工具版本不存在");
        }

        LambdaQueryWrapper<ToolVersionEntity> queryWrapper = Wrappers.<ToolVersionEntity>lambdaQuery()
                .eq(ToolVersionEntity::getToolId, toolId).orderByDesc(ToolVersionEntity::getCreatedAt);

        // 如果当前用户是创建者，返回所有版本；否则只返回公开版本
        if (!userId.equals(tool.getUserId())) {
            queryWrapper.eq(ToolVersionEntity::getPublicStatus, true);
        }

        return toolVersionRepository.selectList(queryWrapper);
    }

    public void updateToolVersionStatus(String toolId, String version, String userId, Boolean publishStatus) {
        Wrapper<ToolVersionEntity> wrapper = Wrappers.<ToolVersionEntity>lambdaUpdate()
                .eq(ToolVersionEntity::getToolId, toolId).eq(ToolVersionEntity::getVersion, version)
                .eq(ToolVersionEntity::getUserId, userId).set(ToolVersionEntity::getPublicStatus, publishStatus);
        toolVersionRepository.checkedUpdate(wrapper);
    }
}
