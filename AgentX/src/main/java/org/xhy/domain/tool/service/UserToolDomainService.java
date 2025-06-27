package org.xhy.domain.tool.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.xhy.domain.tool.model.UserToolEntity;
import org.xhy.domain.tool.repository.UserToolRepository;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.interfaces.dto.tool.request.QueryToolRequest;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 用户已安装工具 service */
@Service
public class UserToolDomainService {

    private final UserToolRepository userToolRepository;

    public UserToolDomainService(UserToolRepository userToolRepository) {
        this.userToolRepository = userToolRepository;
    }

    public void add(UserToolEntity userToolEntity) {
        userToolRepository.checkInsert(userToolEntity);
    }

    public Page<UserToolEntity> listByUserId(String userId, QueryToolRequest queryToolRequest) {
        LambdaQueryWrapper<UserToolEntity> wrapper = Wrappers.<UserToolEntity>lambdaQuery()
                .eq(UserToolEntity::getUserId, userId);
        return userToolRepository.selectPage(new Page<>(queryToolRequest.getPage(), queryToolRequest.getPageSize()),
                wrapper);
    }

    public UserToolEntity findByToolIdAndUserId(String toolId, String userId) {
        LambdaQueryWrapper<UserToolEntity> wrapper = Wrappers.<UserToolEntity>lambdaQuery()
                .eq(UserToolEntity::getToolId, toolId).eq(UserToolEntity::getUserId, userId);
        return userToolRepository.selectOne(wrapper);
    }

    public void update(UserToolEntity userToolEntity) {

        userToolRepository.checkedUpdateById(userToolEntity);
    }

    public void delete(String toolId, String userId) {
        LambdaQueryWrapper<UserToolEntity> wrapper = Wrappers.<UserToolEntity>lambdaQuery()
                .eq(UserToolEntity::getToolId, toolId).eq(UserToolEntity::getUserId, userId);
        userToolRepository.checkedDelete(wrapper);
    }

    // 获取工具的安装次数
    public Map<String, Long> getToolsInstall(List<String> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return new HashMap<>();
        }
        LambdaQueryWrapper<UserToolEntity> wrapper = Wrappers.<UserToolEntity>lambdaQuery()
                .in(UserToolEntity::getToolId, toolIds);
        List<UserToolEntity> userToolEntities = userToolRepository.selectList(wrapper);

        // 根据 userToolEntities 进行 toolId 分组，key toolId，value 是分组数量
        Map<String, Long> toolInstallMap = userToolEntities.stream()
                .collect(Collectors.groupingBy(UserToolEntity::getToolId, Collectors.counting()));

        return toolInstallMap;
    }

    /** 检查工具版本是否已安装
     *
     * @param toolIds 工具版本id列表
     * @param userId 用户id */
    public List<UserToolEntity> getInstallTool(List<String> toolIds, String userId) {
        if (toolIds == null || toolIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<UserToolEntity> userToolEntities = userToolRepository.selectList(Wrappers.<UserToolEntity>lambdaQuery()
                .in(UserToolEntity::getToolId, toolIds).eq(UserToolEntity::getUserId, userId));

        Map<String, UserToolEntity> userToolMap = userToolEntities.stream()
                .collect(Collectors.toMap(UserToolEntity::getToolId, Function.identity()));

        toolIds.forEach(toolId -> {
            UserToolEntity userToolEntity = userToolMap.get(toolId);
            if (userToolEntity == null) {
                throw new BusinessException("使用的工具不存在");
            }
        });
        return userToolEntities;
    }

    /** 根据工具ID列表获取用户安装的工具
     * 
     * @param userId 用户ID
     * @param toolIds 工具ID列表
     * @return 用户安装的工具列表 */
    public List<UserToolEntity> getUserToolsByIds(String userId, List<String> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<UserToolEntity> wrapper = Wrappers.<UserToolEntity>lambdaQuery()
                .eq(UserToolEntity::getUserId, userId)
                .in(UserToolEntity::getToolId, toolIds);

        return userToolRepository.selectList(wrapper);
    }
}
