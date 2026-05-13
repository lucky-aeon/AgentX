package org.xhy.application.tool.service;

import java.util.*;

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
import org.xhy.infrastructure.mcp_gateway.MCPGatewayService;
import org.xhy.infrastructure.utils.JsonUtils;
import org.xhy.interfaces.dto.tool.request.CreateToolRequest;
import org.xhy.interfaces.dto.tool.request.MarketToolRequest;
import org.xhy.interfaces.dto.tool.request.QueryToolRequest;
import org.xhy.interfaces.dto.tool.request.UpdateToolRequest;

@Service
public class ToolAppService {

    private final ToolDomainService toolDomainService;
    private final UserToolDomainService userToolDomainService;
    private final ToolVersionDomainService toolVersionDomainService;
    private final UserDomainService userDomainService;
    private final MCPGatewayService mcpGatewayService;

    public ToolAppService(ToolDomainService toolDomainService, UserToolDomainService userToolDomainService,
            ToolVersionDomainService toolVersionDomainService, UserDomainService userDomainService,
            MCPGatewayService mcpGatewayService) {
        this.toolDomainService = toolDomainService;
        this.userToolDomainService = userToolDomainService;
        this.toolVersionDomainService = toolVersionDomainService;
        this.userDomainService = userDomainService;
        this.mcpGatewayService = mcpGatewayService;
    }

    @Transactional
    public ToolDTO uploadTool(CreateToolRequest request, String userId) {
        // 先把接口层请求转换成领域实体，后续流程都围绕 ToolEntity 展开
        ToolEntity toolEntity = ToolAssembler.toEntity(request, userId);

        // 新工具统一从“待审核”开始，真正的异步处理链由 ToolStateService 推进
        toolEntity.setStatus(ToolStatus.WAITING_REVIEW);
        ToolEntity createdTool = toolDomainService.createTool(toolEntity);

        // 返回前再转成 DTO，避免直接把领域实体暴露给接口层
        return ToolAssembler.toDTO(createdTool);
    }

    public ToolDTO getToolDetail(String toolId, String userId) {
        ToolEntity toolEntity = toolDomainService.getTool(toolId, userId);
        return ToolAssembler.toDTO(toolEntity);
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

        // 只有已经审核通过并完成发布流程的工具，才能创建市场版本
        ToolEntity toolEntity = toolDomainService.getTool(toolId, userId);
        if (toolEntity.getStatus() != ToolStatus.APPROVED) {
            throw new BusinessException("工具尚未审核通过，不能上架到市场");
        }

        ToolVersionEntity toolVersionEntity = toolVersionDomainService.findLatestToolVersion(toolId, userId);
        if (toolVersionEntity != null) {
            // 市场版本号必须单调递增，避免覆盖历史已发布版本
            if (!marketToolRequest.isVersionGreaterThan(toolVersionEntity.getVersion())) {
                throw new ParamValidationException("versionNumber",
                        "新版本号(" + version + ")必须大于当前最新版本号(" + toolVersionEntity.getVersion() + ")");
            }
        }

        // ToolVersionEntity 是“对外发布快照”：
        // 从当前 ToolEntity 复制展示信息和能力定义，再补充版本元数据
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
        UserEntity userInfo = userDomainService.getUserInfo(toolVersionDTO.getUserId());
        toolVersionDTO.setUserName(userInfo.getNickname());

        List<ToolVersionEntity> toolVersionEntities = toolVersionDomainService.getToolVersions(toolId, userId);
        toolVersionDTO.setVersions(toolVersionEntities.stream().map(ToolAssembler::toDTO).toList());

        Map<String, Long> toolsInstall = userToolDomainService.getToolsInstall(Arrays.asList(toolId));
        toolVersionDTO.setInstallCount(toolsInstall.get(toolId));
        return toolVersionDTO;
    }

    public void installTool(String toolId, String version, String userId) {
        UserToolEntity userToolEntity = userToolDomainService.findByToolIdAndUserId(toolId, userId);
        ToolVersionEntity toolVersionEntity = toolVersionDomainService.getToolVersion(toolId, version);
        String currentUserId = userId;

        Map<String, Object> installCommand = toolDomainService.getTool(toolId).getInstallCommand();
        if (installCommand == null || installCommand.isEmpty()) {
            throw new BusinessException("工具安装命令不存在，无法完成部署");
        }
        boolean deploySuccess = mcpGatewayService.deployTool(JsonUtils.toJsonString(installCommand));
        if (!deploySuccess) {
            throw new BusinessException("MCP Gateway 部署失败，安装未完成");
        }

        if (userToolEntity == null) {
            userToolDomainService.purgeByToolIdAndUserId(toolId, userId);
            userToolEntity = new UserToolEntity();
            userToolEntity.setUserId(currentUserId);
            userToolEntity.setToolId(toolVersionEntity.getToolId());
        }
        String userToolId = userToolEntity.getId();
        BeanUtils.copyProperties(toolVersionEntity, userToolEntity);
        // 安装本质上是把市场版本快照复制到用户空间里，形成独立的用户工具记录
        userToolEntity.setVersion(toolVersionEntity.getVersion());
        userToolEntity.setId(userToolId);
        userToolEntity.setUserId(currentUserId);
        userToolEntity.setToolId(toolId);
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
            Random random = new Random();
            toolVersionDTOs = toolVersionDTOs.stream().sorted((a, b) -> random.nextInt(2) - 1).limit(10).toList();
        }

        return toolVersionDTOs;
    }

    public void updateUserToolVersionStatus(String toolId, String version, Boolean publishStatus, String userId) {
        toolVersionDomainService.updateToolVersionStatus(toolId, version, userId, publishStatus);
    }
}
