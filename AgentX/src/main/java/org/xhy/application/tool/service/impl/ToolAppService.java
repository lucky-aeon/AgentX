package org.xhy.application.tool.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.tool.assembler.ToolAssembler;
import org.xhy.application.tool.dto.ToolDTO;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.constant.ToolType;
import org.xhy.domain.tool.constant.UploadType;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.service.ToolDomainService;
import org.xhy.interfaces.dto.tool.request.CreateToolRequest;
import org.xhy.interfaces.dto.tool.request.UpdateToolRequest;

/**
 * 工具应用服务
 */
@Service
public class ToolAppService {

    private final ToolDomainService toolDomainService;

    public ToolAppService(ToolDomainService toolDomainService) {
        this.toolDomainService = toolDomainService;
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

    public void marketTool(String toolId, String userId) {
        toolDomainService.marketTool(toolId, userId);
    }
}