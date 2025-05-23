package org.xhy.application.tool.service;

import java.util.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.tool.assembler.ToolAssembler;
import org.xhy.application.tool.dto.ToolDTO;
import org.xhy.application.tool.dto.ToolVersionDTO;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.model.ToolVersionEntity;
import org.xhy.domain.tool.model.UserToolEntity;
import org.xhy.domain.tool.service.ToolDomainService;
import org.xhy.domain.tool.service.ToolVersionDomainService;
import org.xhy.domain.tool.service.UserToolDomainService;
import org.xhy.domain.user.model.UserEntity;
import org.xhy.domain.user.service.UserDomainService;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.exception.ParamValidationException;
import org.xhy.interfaces.dto.tool.request.CreateToolRequest;
import org.xhy.interfaces.dto.tool.request.MarketToolRequest;
import org.xhy.interfaces.dto.tool.request.QueryToolRequest;
import org.xhy.interfaces.dto.tool.request.UpdateToolRequest;

/** 工具应用服务 */
@Service
public class ToolAppService {

    private static final Logger logger = LoggerFactory.getLogger(ToolAppService.class);

    private final ToolDomainService toolDomainService;

    private final UserToolDomainService userToolDomainService;

    private final ToolVersionDomainService toolVersionDomainService;

    private final UserDomainService userDomainService;

    public ToolAppService(ToolDomainService toolDomainService, UserToolDomainService userToolDomainService,
            ToolVersionDomainService toolVersionDomainService, UserDomainService userDomainService) {
        this.toolDomainService = toolDomainService;
        this.userToolDomainService = userToolDomainService;
        this.toolVersionDomainService = toolVersionDomainService;
        this.userDomainService = userDomainService;
    }

    /**
     * 上传工具
     * 
     * 业务流程： 1. 将请求转换为实体 2. 调用领域服务创建工具 3. 将实体转换为DTO返回
     *
     * @param request 创建工具请求
     * @param userId  用户ID
     * @return 创建的工具DTO
     */
    @Transactional
    public ToolDTO uploadTool(CreateToolRequest request, String userId) {
        // 将请求转换为实体
        ToolEntity toolEntity = ToolAssembler.toEntity(request, userId);

        toolEntity.setStatus(ToolStatus.WAITING_REVIEW);
        // 调用领域服务创建工具
        ToolEntity createdTool = toolDomainService.createTool(toolEntity);

        // 将实体转换为DTO返回
        return ToolAssembler.toDTO(createdTool);
    }

    public ToolDTO getToolDetail(String toolId, String userId) {
        ToolEntity toolEntity = toolDomainService.getTool(toolId, userId);

        ToolDTO toolDTO = ToolAssembler.toDTO(toolEntity);
        return toolDTO;
    }

    public List<ToolDTO> getUserTools(String userId) {
        List<ToolEntity> toolEntities = toolDomainService.getUserTools(userId);
        return ToolAssembler.toDTOs(toolEntities);
    }

    public ToolDTO updateTool(String toolId, UpdateToolRequest request, String userId) {
        ToolEntity toolEntity = ToolAssembler.toEntity(request, userId);
        toolEntity.setId(toolId);
        ToolEntity updatedTool = toolDomainService.updateTool(toolEntity);
        return ToolAssembler.toDTO(updatedTool);
    }

    public void deleteTool(String toolId, String userId) {
        toolDomainService.deleteTool(toolId, userId);
    }

    public void marketTool(MarketToolRequest marketToolRequest, String userId) {
        String toolId = marketToolRequest.getToolId();
        String version = marketToolRequest.getVersion();
        ToolEntity toolEntity = toolDomainService.getTool(toolId, userId);
        // 必须是审核通过才能上架
        if (toolEntity.getStatus() != ToolStatus.APPROVED) {
            throw new BusinessException("工具未审核通过，不能上架");
        }

        ToolVersionEntity toolVersionEntity = toolVersionDomainService.findLatestToolVersion(toolId, userId);
        if (toolVersionEntity != null) {
            // 检查版本号是否大于上一个版本
            if (!marketToolRequest.isVersionGreaterThan(toolVersionEntity.getVersion())) {
                throw new ParamValidationException("versionNumber",
                        "新版本号(" + version + ")必须大于当前最新版本号(" + toolVersionEntity.getVersion() + ")");
            }
        }

        // 创建工具版本进行上架
        toolVersionEntity = new ToolVersionEntity();
        BeanUtils.copyProperties(toolEntity, toolVersionEntity);
        toolVersionEntity.setVersion(version);
        toolVersionEntity.setChangeLog(marketToolRequest.getChangeLog());
        toolVersionEntity.setToolId(toolId);
        toolVersionEntity.setPublicStatus(true);
        toolVersionEntity.setId(null);
        toolVersionEntity.setMcpServerName(toolEntity.getMcpServerName());
        toolVersionDomainService.addToolVersion(toolVersionEntity);
    }

    public Page<ToolVersionDTO> marketTools(QueryToolRequest queryToolRequest) {
        Page<ToolVersionEntity> listToolVersion = toolVersionDomainService.listToolVersion(queryToolRequest);
        List<ToolVersionEntity> records = listToolVersion.getRecords();
        Map<String, Long> toolsInstallMap = userToolDomainService
                .getToolsInstall(records.stream().map(ToolVersionEntity::getToolId).toList());
        List<ToolVersionDTO> list = records.stream().map(toolVersionEntity -> {
            ToolVersionDTO toolVersionDTO = ToolAssembler.toDTO(toolVersionEntity);
            toolVersionDTO.setInstallCount(toolsInstallMap.get(toolVersionEntity.getToolId()));
            return toolVersionDTO;
        }).toList();
        Page<ToolVersionDTO> tPage = new Page<>(listToolVersion.getCurrent(), listToolVersion.getSize(),
                listToolVersion.getTotal());
        tPage.setRecords(list);
        return tPage;
    }

    public ToolVersionDTO getToolVersionDetail(String toolId, String version, String userId) {
        ToolVersionEntity toolVersionEntity = toolVersionDomainService.getToolVersion(toolId, version);
        ToolVersionDTO toolVersionDTO = ToolAssembler.toDTO(toolVersionEntity);
        // 设置创建者昵称
        UserEntity userInfo = userDomainService.getUserInfo(toolVersionDTO.getUserId());
        toolVersionDTO.setUserName(userInfo.getNickname());

        // 设置历史版本
        List<ToolVersionEntity> toolVersionEntities = toolVersionDomainService.getToolVersions(toolId, userId);
        toolVersionDTO.setVersions(toolVersionEntities.stream().map(ToolAssembler::toDTO).toList());

        Map<String, Long> toolsInstall = userToolDomainService.getToolsInstall(Arrays.asList(toolId));
        toolVersionDTO.setInstallCount(toolsInstall.get(toolId));
        return toolVersionDTO;
    }

    public void installTool(String toolId, String version, String userId) {
        UserToolEntity userToolEntity = userToolDomainService.findByToolIdAndUserId(toolId, userId);
        ToolVersionEntity toolVersionEntity = toolVersionDomainService.getToolVersion(toolId, version);

        if (userToolEntity == null) {
            userToolEntity = new UserToolEntity();
            userToolEntity.setUserId(userId);
            userToolEntity.setToolId(toolVersionEntity.getToolId());
        }
        String userToolId = userToolEntity.getId();
        BeanUtils.copyProperties(toolVersionEntity, userToolEntity);
        // 使用工具版本实体更新用户工具实体的信息
        userToolEntity.setVersion(toolVersionEntity.getVersion());
        userToolEntity.setId(userToolId);
        userToolEntity.setMcpServerName(toolVersionEntity.getMcpServerName());
        if (userToolEntity.getId() == null) {
            userToolDomainService.add(userToolEntity);
        } else {
            userToolDomainService.update(userToolEntity);
        }
    }

    public Page<ToolVersionDTO> getInstalledTools(String userId, QueryToolRequest queryToolRequest) {

        Page<UserToolEntity> userToolEntityPage = userToolDomainService.listByUserId(userId, queryToolRequest);
        List<ToolVersionDTO> list = userToolEntityPage.getRecords().stream().map(ToolAssembler::toDTO).toList();
        Page<ToolVersionDTO> tPage = new Page<>(userToolEntityPage.getCurrent(), userToolEntityPage.getSize(),
                userToolEntityPage.getTotal());
        tPage.setRecords(list);
        return tPage;
    }

    public List<ToolVersionDTO> getToolVersions(String toolId, String userId) {
        List<ToolVersionEntity> toolVersionEntities = toolVersionDomainService.getToolVersions(toolId, userId);
        return toolVersionEntities.stream().map(ToolAssembler::toDTO).toList();
    }

    public void uninstallTool(String toolId, String userId) {
        // 先检查是否是用户自己创建的工具
        ToolEntity toolEntity = toolDomainService.getTool(toolId);
        
        if (toolEntity != null && toolEntity.getUserId().equals(userId)) {
            // 不允许删除用户自己创建的工具
            throw new BusinessException("不允许卸载自己创建的工具");
        }
        
        // 执行正常的卸载流程
        userToolDomainService.delete(toolId, userId);
    }

    public List<ToolVersionDTO> getRecommendTools() {
        QueryToolRequest queryToolRequest = new QueryToolRequest();
        queryToolRequest.setPage(1);
        queryToolRequest.setPageSize(Integer.MAX_VALUE);
        Page<ToolVersionEntity> listToolVersion = toolVersionDomainService.listToolVersion(queryToolRequest);
        List<ToolVersionEntity> records = listToolVersion.getRecords();

        Map<String, Long> toolsInstallMap = userToolDomainService
                .getToolsInstall(records.stream().map(ToolVersionEntity::getToolId).toList());

        List<ToolVersionDTO> toolVersionDTOs = records.stream().map(toolVersionEntity -> {
            ToolVersionDTO dto = ToolAssembler.toDTO(toolVersionEntity);
            dto.setInstallCount(toolsInstallMap.get(dto.getToolId()));
            return dto;
        }).toList();

        if (records.size() > 10) {
            // 使用随机数从所有记录中选取10条不重复的记录
            Random random = new Random();
            toolVersionDTOs = toolVersionDTOs.stream().sorted((a, b) -> random.nextInt(2) - 1).limit(10).toList();
        }

        return toolVersionDTOs;
    }

    public void updateUserToolVersionStatus(String toolId, String version, Boolean publishStatus, String userId) {
        toolVersionDomainService.updateToolVersionStatus(toolId, version, userId, publishStatus);
    }

    /**
     * 为工具创建者自动安装审核通过的工具
     * @param toolId 工具ID
     */
    public void autoInstallApprovedTool(String toolId) {
        ToolEntity tool = toolDomainService.getTool(toolId);
        // 确保工具存在且已审核通过
        if (tool == null || tool.getStatus() != ToolStatus.APPROVED) {
            logger.warn("工具ID: {} 不存在或状态不是 APPROVED，无法自动安装。", toolId);
            return;
        }
        
        String ownerId = tool.getUserId(); // 获取工具创建者ID
        
        // 检查是否已安装
        UserToolEntity existingInstall = userToolDomainService.findByToolIdAndUserId(toolId, ownerId);
        if (existingInstall != null) {
            logger.info("工具ID: {} 已被用户ID: {} 安装，无需重复自动安装。版本: {}", toolId, ownerId, existingInstall.getVersion());
            return;
        }
        
        // 尝试查找最新已发布的版本
        ToolVersionEntity versionToInstall = toolVersionDomainService.findLatestToolVersion(toolId, ownerId);
        
        if (versionToInstall == null) {
            // 没有已发布的版本，为创建者创建一个代表基础配置的、非公开的内部版本记录
            logger.info("工具ID: {} 未找到任何已发布版本，为其创建者 {} 创建一个内部基础版本用于安装。", toolId, ownerId);
            ToolVersionEntity baseVersion = new ToolVersionEntity();
            BeanUtils.copyProperties(tool, baseVersion); // 从ToolEntity复制基础信息
            baseVersion.setId(null); //确保是新记录
            baseVersion.setToolId(toolId);
            baseVersion.setUserId(ownerId); // 版本归属创建者
            baseVersion.setVersion("0.0.0-base"); // 特殊版本号
            baseVersion.setChangeLog("Base configuration for owner auto-installation.");
            baseVersion.setPublicStatus(false); // 非公开
            // baseVersion.setInstallCommand(tool.getInstallCommand()); // 如果ToolVersionEntity需要此字段且ToolEntity有，则复制
            baseVersion.setMcpServerName(tool.getMcpServerName());

            // 持久化这个内部基础版本
            toolVersionDomainService.addToolVersion(baseVersion);
            versionToInstall = baseVersion;
            logger.info("工具ID: {} 已成功创建内部基础版本: {}", toolId, versionToInstall.getVersion());
        }
        
        logger.info("准备为用户ID: {} 安装工具ID: {} 的版本: {}", ownerId, toolId, versionToInstall.getVersion());
        installTool(toolId, versionToInstall.getVersion(), ownerId);
        logger.info("工具ID: {} 版本: {} 已成功为创建者用户ID: {} 自动安装。", toolId, versionToInstall.getVersion(), ownerId);
    }

}