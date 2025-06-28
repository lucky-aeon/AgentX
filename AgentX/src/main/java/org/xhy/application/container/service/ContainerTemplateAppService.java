package org.xhy.application.container.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.container.assembler.ContainerTemplateAssembler;
import org.xhy.application.container.dto.ContainerTemplateDTO;
import org.xhy.interfaces.dto.container.request.CreateContainerTemplateRequest;
import org.xhy.interfaces.dto.container.request.UpdateContainerTemplateRequest;
import org.xhy.interfaces.dto.container.request.QueryContainerTemplateRequest;
import org.xhy.domain.container.model.ContainerTemplateEntity;
import org.xhy.domain.container.service.ContainerTemplateDomainService;
import org.xhy.infrastructure.entity.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** 容器模板应用服务 */
@Service
public class ContainerTemplateAppService {

    private static final Logger logger = LoggerFactory.getLogger(ContainerTemplateAppService.class);

    private final ContainerTemplateDomainService templateDomainService;

    public ContainerTemplateAppService(ContainerTemplateDomainService templateDomainService) {
        this.templateDomainService = templateDomainService;
    }

    /** 创建容器模板
     * 
     * @param request 创建请求
     * @param userId 创建者用户ID
     * @return 模板信息 */
    @Transactional
    public ContainerTemplateDTO createTemplate(CreateContainerTemplateRequest request, String userId) {
        ContainerTemplateEntity template = ContainerTemplateAssembler.toEntity(request, userId);
        ContainerTemplateEntity createdTemplate = templateDomainService.createTemplate(template, Operator.USER);

        logger.info("用户{}创建容器模板: {}", userId, createdTemplate.getName());
        return ContainerTemplateAssembler.toDTO(createdTemplate);
    }

    /** 更新容器模板
     * 
     * @param templateId 模板ID
     * @param request 更新请求
     * @param userId 操作用户ID
     * @return 更新后的模板信息 */
    @Transactional
    public ContainerTemplateDTO updateTemplate(String templateId, UpdateContainerTemplateRequest request,
            String userId) {
        ContainerTemplateEntity updates = ContainerTemplateAssembler.toEntity(request);
        ContainerTemplateEntity updatedTemplate = templateDomainService.updateTemplate(templateId, updates,
                Operator.USER);

        logger.info("用户{}更新容器模板: {}", userId, updatedTemplate.getName());
        return ContainerTemplateAssembler.toDTO(updatedTemplate);
    }

    /** 删除容器模板
     * 
     * @param templateId 模板ID
     * @param userId 操作用户ID */
    @Transactional
    public void deleteTemplate(String templateId, String userId) {
        templateDomainService.deleteTemplate(templateId, Operator.USER);
        logger.info("用户{}删除容器模板: {}", userId, templateId);
    }

    /** 启用/禁用模板
     * 
     * @param templateId 模板ID
     * @param enabled 是否启用
     * @param userId 操作用户ID */
    @Transactional
    public void toggleTemplateStatus(String templateId, boolean enabled, String userId) {
        templateDomainService.toggleTemplateStatus(templateId, enabled, Operator.USER);
        logger.info("用户{}{}容器模板: {}", userId, enabled ? "启用" : "禁用", templateId);
    }

    /** 设置默认模板
     * 
     * @param templateId 模板ID
     * @param userId 操作用户ID */
    @Transactional
    public void setDefaultTemplate(String templateId, String userId) {
        templateDomainService.setDefaultTemplate(templateId, Operator.USER);
        logger.info("用户{}设置默认容器模板: {}", userId, templateId);
    }

    /** 根据ID获取模板
     * 
     * @param templateId 模板ID
     * @return 模板信息 */
    public ContainerTemplateDTO getTemplate(String templateId) {
        ContainerTemplateEntity template = templateDomainService.getTemplateById(templateId);
        return ContainerTemplateAssembler.toDTO(template);
    }

    /** 根据类型获取默认模板
     * 
     * @param type 模板类型
     * @return 默认模板信息，可能为null */
    public ContainerTemplateDTO getDefaultTemplate(String type) {
        ContainerTemplateEntity template = templateDomainService.getDefaultTemplate(type);
        return template != null ? ContainerTemplateAssembler.toDTO(template) : null;
    }

    /** 获取MCP网关默认模板 */
    public ContainerTemplateDTO getMcpGatewayTemplate() {
        ContainerTemplateEntity template = templateDomainService.getMcpGatewayTemplate();
        return ContainerTemplateAssembler.toDTO(template);
    }

    /** 根据类型获取所有模板
     * 
     * @param type 模板类型
     * @return 模板列表 */
    public List<ContainerTemplateDTO> getTemplatesByType(String type) {
        List<ContainerTemplateEntity> templates = templateDomainService.getTemplatesByType(type);
        return ContainerTemplateAssembler.toDTOs(templates);
    }

    /** 获取所有启用的模板
     * 
     * @return 模板列表 */
    public List<ContainerTemplateDTO> getEnabledTemplates() {
        List<ContainerTemplateEntity> templates = templateDomainService.getEnabledTemplates();
        return ContainerTemplateAssembler.toDTOs(templates);
    }

    /** 分页查询模板
     * 
     * @param queryRequest 查询条件
     * @return 分页结果 */
    public Page<ContainerTemplateDTO> getTemplatesPage(QueryContainerTemplateRequest queryRequest) {
        Page<ContainerTemplateEntity> page = new Page<>(queryRequest.getPage(), queryRequest.getPageSize());

        Page<ContainerTemplateEntity> entityPage = templateDomainService.getTemplatesPage(page,
                queryRequest.getKeyword(), queryRequest.getType(), queryRequest.getEnabled());

        return ContainerTemplateAssembler.toDTOPage(entityPage);
    }

    /** 获取模板统计信息
     * 
     * @return 统计信息 */
    public ContainerTemplateDomainService.TemplateStatistics getStatistics() {
        return templateDomainService.getStatistics();
    }

    // ============== 管理员功能 ==============

    /** 管理员创建模板
     * 
     * @param request 创建请求
     * @return 模板信息 */
    @Transactional
    public ContainerTemplateDTO adminCreateTemplate(CreateContainerTemplateRequest request) {
        ContainerTemplateEntity template = ContainerTemplateAssembler.toEntity(request, "ADMIN");
        ContainerTemplateEntity createdTemplate = templateDomainService.createTemplate(template, Operator.ADMIN);

        logger.info("管理员创建容器模板: {}", createdTemplate.getName());
        return ContainerTemplateAssembler.toDTO(createdTemplate);
    }

    /** 管理员更新模板
     * 
     * @param templateId 模板ID
     * @param request 更新请求
     * @return 更新后的模板信息 */
    @Transactional
    public ContainerTemplateDTO adminUpdateTemplate(String templateId, UpdateContainerTemplateRequest request) {
        ContainerTemplateEntity updates = ContainerTemplateAssembler.toEntity(request);
        ContainerTemplateEntity updatedTemplate = templateDomainService.updateTemplate(templateId, updates,
                Operator.ADMIN);

        logger.info("管理员更新容器模板: {}", updatedTemplate.getName());
        return ContainerTemplateAssembler.toDTO(updatedTemplate);
    }

    /** 管理员删除模板
     * 
     * @param templateId 模板ID */
    @Transactional
    public void adminDeleteTemplate(String templateId) {
        templateDomainService.deleteTemplate(templateId, Operator.ADMIN);
        logger.info("管理员删除容器模板: {}", templateId);
    }

    /** 管理员启用/禁用模板
     * 
     * @param templateId 模板ID
     * @param enabled 是否启用 */
    @Transactional
    public void adminToggleTemplateStatus(String templateId, boolean enabled) {
        templateDomainService.toggleTemplateStatus(templateId, enabled, Operator.ADMIN);
        logger.info("管理员{}容器模板: {}", enabled ? "启用" : "禁用", templateId);
    }

    /** 管理员设置默认模板
     * 
     * @param templateId 模板ID */
    @Transactional
    public void adminSetDefaultTemplate(String templateId) {
        templateDomainService.setDefaultTemplate(templateId, Operator.ADMIN);
        logger.info("管理员设置默认容器模板: {}", templateId);
    }
}