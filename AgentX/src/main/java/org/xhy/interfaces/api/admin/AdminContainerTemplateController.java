package org.xhy.interfaces.api.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.container.dto.ContainerTemplateDTO;
import org.xhy.application.container.service.ContainerTemplateAppService;
import org.xhy.domain.container.constant.ContainerType;
import org.xhy.interfaces.dto.container.request.CreateContainerTemplateRequest;
import org.xhy.interfaces.dto.container.request.UpdateContainerTemplateRequest;
import org.xhy.interfaces.dto.container.request.QueryContainerTemplateRequest;
import org.xhy.domain.container.service.ContainerTemplateDomainService;
import org.xhy.interfaces.api.common.Result;

import java.util.List;

/** 管理员容器模板管理控制器 */
@RestController
@RequestMapping("/admin/container-templates")
public class AdminContainerTemplateController {

    private final ContainerTemplateAppService templateAppService;

    public AdminContainerTemplateController(ContainerTemplateAppService templateAppService) {
        this.templateAppService = templateAppService;
    }

    /** 分页获取容器模板列表
     * 
     * @param queryRequest 查询参数
     * @return 模板分页列表 */
    @GetMapping
    public Result<Page<ContainerTemplateDTO>> getTemplates(QueryContainerTemplateRequest queryRequest) {
        Page<ContainerTemplateDTO> templatePage = templateAppService.getTemplatesPage(queryRequest);
        return Result.success(templatePage);
    }

    /** 获取容器模板详情
     * 
     * @param templateId 模板ID
     * @return 模板详情 */
    @GetMapping("/{templateId}")
    public Result<ContainerTemplateDTO> getTemplate(@PathVariable String templateId) {
        ContainerTemplateDTO template = templateAppService.getTemplate(templateId);
        return Result.success(template);
    }

    /** 根据类型获取模板列表
     * 
     * @param type 模板类型
     * @return 模板列表 */
    @GetMapping("/by-type/{type}")
    public Result<List<ContainerTemplateDTO>> getTemplatesByType(@PathVariable String type) {
        List<ContainerTemplateDTO> templates = templateAppService.getTemplatesByType(type);
        return Result.success(templates);
    }

    /** 获取默认模板
     * 
     * @param type 模板类型
     * @return 默认模板 */
    @GetMapping("/default/{type}")
    public Result<ContainerTemplateDTO> getDefaultTemplate(@PathVariable String type) {

        ContainerTemplateDTO template = templateAppService.getDefaultTemplate(ContainerType.fromCode(type));
        return Result.success(template);
    }

    /** 获取所有启用的模板
     * 
     * @return 启用的模板列表 */
    @GetMapping("/enabled")
    public Result<List<ContainerTemplateDTO>> getEnabledTemplates() {
        List<ContainerTemplateDTO> templates = templateAppService.getEnabledTemplates();
        return Result.success(templates);
    }

    /** 创建容器模板
     * 
     * @param request 创建请求
     * @return 创建的模板信息 */
    @PostMapping
    public Result<ContainerTemplateDTO> createTemplate(@RequestBody @Validated CreateContainerTemplateRequest request) {
        ContainerTemplateDTO template = templateAppService.adminCreateTemplate(request);
        return Result.success(template);
    }

    /** 更新容器模板
     * 
     * @param templateId 模板ID
     * @param request 更新请求
     * @return 更新后的模板信息 */
    @PutMapping("/{templateId}")
    public Result<ContainerTemplateDTO> updateTemplate(@PathVariable String templateId,
            @RequestBody @Validated UpdateContainerTemplateRequest request) {
        ContainerTemplateDTO template = templateAppService.adminUpdateTemplate(templateId, request);
        return Result.success(template);
    }

    /** 删除容器模板
     * 
     * @param templateId 模板ID
     * @return 操作结果 */
    @DeleteMapping("/{templateId}")
    public Result<Void> deleteTemplate(@PathVariable String templateId) {
        templateAppService.adminDeleteTemplate(templateId);
        return Result.success();
    }

    /** 启用/禁用模板
     * 
     * @param templateId 模板ID
     * @param enabled 是否启用
     * @return 操作结果 */
    @PutMapping("/{templateId}/status")
    public Result<Void> toggleTemplateStatus(@PathVariable String templateId, @RequestParam Boolean enabled) {
        templateAppService.adminToggleTemplateStatus(templateId, enabled);
        return Result.success();
    }

    /** 设置默认模板
     * 
     * @param templateId 模板ID
     * @return 操作结果 */
    @PutMapping("/{templateId}/default")
    public Result<Void> setDefaultTemplate(@PathVariable String templateId) {
        templateAppService.adminSetDefaultTemplate(templateId);
        return Result.success();
    }

    /** 获取模板统计信息
     * 
     * @return 统计信息 */
    @GetMapping("/statistics")
    public Result<ContainerTemplateDomainService.TemplateStatistics> getStatistics() {
        ContainerTemplateDomainService.TemplateStatistics statistics = templateAppService.getStatistics();
        return Result.success(statistics);
    }
}