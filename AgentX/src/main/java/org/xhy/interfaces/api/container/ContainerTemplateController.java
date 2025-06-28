package org.xhy.interfaces.api.container;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.container.dto.ContainerTemplateDTO;
import org.xhy.application.container.service.ContainerTemplateAppService;
import org.xhy.interfaces.dto.container.request.CreateContainerTemplateRequest;
import org.xhy.interfaces.dto.container.request.UpdateContainerTemplateRequest;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;

import java.util.List;

/** 用户容器模板控制器 */
@RestController
@RequestMapping("/container-templates")
public class ContainerTemplateController {

    private final ContainerTemplateAppService templateAppService;

    public ContainerTemplateController(ContainerTemplateAppService templateAppService) {
        this.templateAppService = templateAppService;
    }

    /** 获取MCP网关默认模板
     * 
     * @return 默认模板 */
    @GetMapping("/mcp-gateway")
    public Result<ContainerTemplateDTO> getMcpGatewayTemplate() {
        ContainerTemplateDTO template = templateAppService.getMcpGatewayTemplate();
        return Result.success(template);
    }

    /** 根据类型获取启用的模板列表
     * 
     * @param type 模板类型
     * @return 模板列表 */
    @GetMapping("/enabled/by-type/{type}")
    public Result<List<ContainerTemplateDTO>> getEnabledTemplatesByType(@PathVariable String type) {
        List<ContainerTemplateDTO> templates = templateAppService.getTemplatesByType(type);
        return Result.success(templates);
    }

    /** 获取所有启用的模板
     * 
     * @return 启用的模板列表 */
    @GetMapping("/enabled")
    public Result<List<ContainerTemplateDTO>> getEnabledTemplates() {
        List<ContainerTemplateDTO> templates = templateAppService.getEnabledTemplates();
        return Result.success(templates);
    }

    /** 获取默认模板
     * 
     * @param type 模板类型
     * @return 默认模板 */
    @GetMapping("/default/{type}")
    public Result<ContainerTemplateDTO> getDefaultTemplate(@PathVariable String type) {
        ContainerTemplateDTO template = templateAppService.getDefaultTemplate(type);
        return Result.success(template);
    }

    /** 获取模板详情
     * 
     * @param templateId 模板ID
     * @return 模板详情 */
    @GetMapping("/{templateId}")
    public Result<ContainerTemplateDTO> getTemplate(@PathVariable String templateId) {
        ContainerTemplateDTO template = templateAppService.getTemplate(templateId);
        return Result.success(template);
    }

    /** 创建自定义模板（如果允许用户创建）
     * 
     * @param request 创建请求
     * @return 创建的模板信息 */
    @PostMapping
    public Result<ContainerTemplateDTO> createTemplate(@RequestBody @Validated CreateContainerTemplateRequest request) {
        String userId = UserContext.getCurrentUserId();
        ContainerTemplateDTO template = templateAppService.createTemplate(request, userId);
        return Result.success(template);
    }

    /** 更新自定义模板
     * 
     * @param templateId 模板ID
     * @param request 更新请求
     * @return 更新后的模板信息 */
    @PutMapping("/{templateId}")
    public Result<ContainerTemplateDTO> updateTemplate(@PathVariable String templateId,
            @RequestBody @Validated UpdateContainerTemplateRequest request) {
        String userId = UserContext.getCurrentUserId();
        ContainerTemplateDTO template = templateAppService.updateTemplate(templateId, request, userId);
        return Result.success(template);
    }

    /** 删除自定义模板
     * 
     * @param templateId 模板ID
     * @return 操作结果 */
    @DeleteMapping("/{templateId}")
    public Result<Void> deleteTemplate(@PathVariable String templateId) {
        String userId = UserContext.getCurrentUserId();
        templateAppService.deleteTemplate(templateId, userId);
        return Result.success();
    }
}