package org.xhy.application.tool.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

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

/**
 * 工具应用服务
 */
@Service
public class ToolAppService {

    private final ToolDomainService toolDomainService;

    private final UserToolDomainService userToolDomainService;  

    private final ToolVersionDomainService toolVersionDomainService;

    private final UserDomainService userDomainService;

    public ToolAppService(ToolDomainService toolDomainService, UserToolDomainService userToolDomainService, ToolVersionDomainService toolVersionDomainService, UserDomainService userDomainService) {
        this.toolDomainService = toolDomainService;
        this.userToolDomainService = userToolDomainService;
        this.toolVersionDomainService = toolVersionDomainService;
        this.userDomainService = userDomainService;
    }

    /**
     * 上传工具
     * 
     * 业务流程：
     * 1. 将请求转换为实体
     * 2. 调用领域服务创建工具
     * 3. 将实体转换为DTO返回
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

        ToolVersionEntity toolVersionEntity = toolVersionDomainService.findLatestToolVersion(toolId,userId);
        if (toolVersionEntity!=null){
            // 检查版本号是否大于上一个版本
            if (!marketToolRequest.isVersionGreaterThan(toolVersionEntity.getVersion())) {
                throw new ParamValidationException("versionNumber", "新版本号(" + version
                        + ")必须大于当前最新版本号(" + toolVersionEntity.getVersion() + ")");
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
        toolVersionDomainService.addToolVersion(toolVersionEntity);
    }

    public Page<ToolVersionDTO> marketTools(QueryToolRequest queryToolRequest) {
        Page<ToolVersionEntity> listToolVersion = toolVersionDomainService.listToolVersion(queryToolRequest);;
        List<ToolVersionDTO> list = listToolVersion.getRecords().stream().map(ToolAssembler::toDTO).toList();
        Page<ToolVersionDTO> tPage = new Page<>(listToolVersion.getCurrent(), listToolVersion.getSize(), listToolVersion.getTotal());
        tPage.setRecords(list);
        return tPage;
    }

    public ToolVersionDTO getToolVersionDetail(String toolId, String version) {
        ToolVersionEntity toolVersionEntity = toolVersionDomainService.getToolVersion(toolId, version);
        ToolVersionDTO toolVersionDTO = ToolAssembler.toDTO(toolVersionEntity);
        // 设置创建者昵称
        UserEntity userInfo = userDomainService.getUserInfo(toolVersionDTO.getUserId());
        toolVersionDTO.setUserName(userInfo.getNickname());

        // 设置历史版本
        List<ToolVersionEntity> toolVersionEntities = toolVersionDomainService.getToolVersions(toolId);
        toolVersionDTO.setVersions(toolVersionEntities.stream().map(ToolAssembler::toDTO).toList());
        return toolVersionDTO;
    }

    public void installTool(String toolId, String version, String userId) {

        ToolVersionEntity toolVersionEntity = toolVersionDomainService.getToolVersion(toolId, version);
        // 检查是否已安装
        userToolDomainService.checkUserToolExist(userId, toolVersionEntity.getId());
        // 安装工具
        UserToolEntity userToolEntity = new UserToolEntity();
        BeanUtils.copyProperties(toolVersionEntity, userToolEntity);
        userToolEntity.setUserId(userId);
        userToolEntity.setToolVersionId(toolVersionEntity.getId());
        userToolDomainService.add(userToolEntity);
    }

    public Page<ToolDTO> getInstalledTools(String userId, QueryToolRequest queryToolRequest) {
        Page<UserToolEntity> userToolEntityPage = userToolDomainService.listByUserId(userId, queryToolRequest);
        List<ToolDTO> list = userToolEntityPage.getRecords().stream().map(ToolAssembler::toDTO).toList();
        Page<ToolDTO> tPage = new Page<>(userToolEntityPage.getCurrent(), userToolEntityPage.getSize(), userToolEntityPage.getTotal());
        tPage.setRecords(list);
        return tPage;
    }
}