package org.xhy.domain.container.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.domain.container.model.ContainerTemplateEntity;
import org.xhy.domain.container.repository.ContainerTemplateRepository;
import org.xhy.infrastructure.entity.Operator;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.List;

/** 容器模板领域服务 */
@Service
public class ContainerTemplateDomainService {

    /** MCP网关模板类型（即用户容器） */
    public static final String MCP_GATEWAY_TYPE = "mcp-gateway";

    private final ContainerTemplateRepository templateRepository;

    public ContainerTemplateDomainService(ContainerTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /** 创建容器模板
     * 
     * @param template 模板实体
     * @param operator 操作者
     * @return 创建的模板 */
    @Transactional
    public ContainerTemplateEntity createTemplate(ContainerTemplateEntity template, Operator operator) {
        // 验证模板有效性
        if (!template.isValid()) {
            throw new BusinessException("模板配置无效");
        }

        // 检查模板名称是否已存在
        if (templateRepository.existsByName(template.getName(), null)) {
            throw new BusinessException("模板名称已存在");
        }

        // 如果设置为默认模板，需要取消同类型的其他默认模板
        if (Boolean.TRUE.equals(template.getIsDefault())) {
            clearDefaultTemplate(template.getType());
        }

        // 设置默认值
        if (template.getEnabled() == null) {
            template.setEnabled(true);
        }
        if (template.getSortOrder() == null) {
            template.setSortOrder(0);
        }

        templateRepository.insert(template);
        return template;
    }

    /** 更新容器模板
     * 
     * @param templateId 模板ID
     * @param updates 更新内容
     * @param operator 操作者
     * @return 更新后的模板 */
    @Transactional
    public ContainerTemplateEntity updateTemplate(String templateId, ContainerTemplateEntity updates, 
                                                 Operator operator) {
        ContainerTemplateEntity existingTemplate = templateRepository.selectById(templateId);
        if (existingTemplate == null) {
            throw new BusinessException("模板不存在");
        }

        // 检查模板名称冲突
        if (updates.getName() != null && 
            !updates.getName().equals(existingTemplate.getName()) &&
            templateRepository.existsByName(updates.getName(), templateId)) {
            throw new BusinessException("模板名称已存在");
        }

        // 更新字段
        updateTemplateFields(existingTemplate, updates);

        // 验证更新后的模板
        if (!existingTemplate.isValid()) {
            throw new BusinessException("更新后的模板配置无效");
        }

        // 如果设置为默认模板，需要取消同类型的其他默认模板
        if (Boolean.TRUE.equals(existingTemplate.getIsDefault())) {
            clearDefaultTemplate(existingTemplate.getType());
        }

        templateRepository.updateById(existingTemplate);
        return existingTemplate;
    }

    /** 删除容器模板
     * 
     * @param templateId 模板ID
     * @param operator 操作者 */
    @Transactional
    public void deleteTemplate(String templateId, Operator operator) {
        ContainerTemplateEntity template = templateRepository.selectById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        // 检查是否有容器正在使用此模板
        // TODO: 在容器管理模块中添加检查逻辑

        templateRepository.deleteById(templateId);
    }

    /** 启用/禁用模板
     * 
     * @param templateId 模板ID
     * @param enabled 是否启用
     * @param operator 操作者 */
    @Transactional
    public void toggleTemplateStatus(String templateId, boolean enabled, Operator operator) {
        ContainerTemplateEntity template = templateRepository.selectById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        template.setEnabled(enabled);
        
        // 如果禁用的是默认模板，需要取消默认设置
        if (!enabled && Boolean.TRUE.equals(template.getIsDefault())) {
            template.setIsDefault(false);
        }

        templateRepository.updateById(template);
    }

    /** 设置默认模板
     * 
     * @param templateId 模板ID
     * @param operator 操作者 */
    @Transactional
    public void setDefaultTemplate(String templateId, Operator operator) {
        ContainerTemplateEntity template = templateRepository.selectById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        if (!template.getEnabled()) {
            throw new BusinessException("只能设置启用的模板为默认模板");
        }

        // 取消同类型的其他默认模板
        clearDefaultTemplate(template.getType());

        // 设置当前模板为默认
        template.setIsDefault(true);
        templateRepository.updateById(template);
    }

    /** 根据类型获取默认模板
     * 
     * @param type 模板类型
     * @return 默认模板，可能为null */
    public ContainerTemplateEntity getDefaultTemplate(String type) {
        return templateRepository.findDefaultByType(type);
    }

    /** 获取MCP网关默认模板 */
    public ContainerTemplateEntity getMcpGatewayTemplate() {
        ContainerTemplateEntity template = getDefaultTemplate(MCP_GATEWAY_TYPE);
        if (template == null) {
            // 如果没有配置默认模板，返回内置模板
            return createBuiltinMcpGatewayTemplate();
        }
        return template;
    }

    /** 根据类型获取所有模板
     * 
     * @param type 模板类型
     * @return 模板列表 */
    public List<ContainerTemplateEntity> getTemplatesByType(String type) {
        return templateRepository.findByType(type);
    }

    /** 获取所有启用的模板
     * 
     * @return 模板列表 */
    public List<ContainerTemplateEntity> getEnabledTemplates() {
        return templateRepository.findEnabledTemplates();
    }

    /** 根据ID获取模板
     * 
     * @param templateId 模板ID
     * @return 模板实体 */
    public ContainerTemplateEntity getTemplateById(String templateId) {
        ContainerTemplateEntity template = templateRepository.selectById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        return template;
    }

    /** 分页查询模板
     * 
     * @param page 分页参数
     * @param keyword 搜索关键词
     * @param type 模板类型
     * @param enabled 是否启用
     * @return 分页结果 */
    public Page<ContainerTemplateEntity> getTemplatesPage(Page<ContainerTemplateEntity> page, 
                                                         String keyword, String type, Boolean enabled) {
        return templateRepository.selectPageWithConditions(page, keyword, type, enabled);
    }

    /** 获取模板统计信息 */
    public TemplateStatistics getStatistics() {
        long totalTemplates = templateRepository.countTemplates();
        long enabledTemplates = templateRepository.countEnabledTemplates();
        
        return new TemplateStatistics(totalTemplates, enabledTemplates);
    }

    /** 取消指定类型的默认模板设置 */
    private void clearDefaultTemplate(String type) {
        ContainerTemplateEntity currentDefault = templateRepository.findDefaultByType(type);
        if (currentDefault != null) {
            currentDefault.setIsDefault(false);
            templateRepository.updateById(currentDefault);
        }
    }

    /** 更新模板字段 */
    private void updateTemplateFields(ContainerTemplateEntity existing, ContainerTemplateEntity updates) {
        if (updates.getName() != null) {
            existing.setName(updates.getName());
        }
        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }
        if (updates.getType() != null) {
            existing.setType(updates.getType());
        }
        if (updates.getImage() != null) {
            existing.setImage(updates.getImage());
        }
        if (updates.getImageTag() != null) {
            existing.setImageTag(updates.getImageTag());
        }
        if (updates.getInternalPort() != null) {
            existing.setInternalPort(updates.getInternalPort());
        }
        if (updates.getCpuLimit() != null) {
            existing.setCpuLimit(updates.getCpuLimit());
        }
        if (updates.getMemoryLimit() != null) {
            existing.setMemoryLimit(updates.getMemoryLimit());
        }
        if (updates.getEnvironment() != null) {
            existing.setEnvironment(updates.getEnvironment());
        }
        if (updates.getVolumeMountPath() != null) {
            existing.setVolumeMountPath(updates.getVolumeMountPath());
        }
        if (updates.getCommand() != null) {
            existing.setCommand(updates.getCommand());
        }
        if (updates.getNetworkMode() != null) {
            existing.setNetworkMode(updates.getNetworkMode());
        }
        if (updates.getRestartPolicy() != null) {
            existing.setRestartPolicy(updates.getRestartPolicy());
        }
        if (updates.getHealthCheck() != null) {
            existing.setHealthCheck(updates.getHealthCheck());
        }
        if (updates.getResourceConfig() != null) {
            existing.setResourceConfig(updates.getResourceConfig());
        }
        if (updates.getEnabled() != null) {
            existing.setEnabled(updates.getEnabled());
        }
        if (updates.getIsDefault() != null) {
            existing.setIsDefault(updates.getIsDefault());
        }
        if (updates.getSortOrder() != null) {
            existing.setSortOrder(updates.getSortOrder());
        }
    }

    /** 创建内置MCP网关模板 */
    private ContainerTemplateEntity createBuiltinMcpGatewayTemplate() {
        ContainerTemplateEntity template = new ContainerTemplateEntity();
        template.setName("MCP网关默认模板");
        template.setDescription("内置的MCP网关容器模板，用于用户容器创建");
        template.setType(MCP_GATEWAY_TYPE);
        template.setImage("ghcr.io/lucky-aeon/mcp-gateway");
        template.setImageTag("latest");
        template.setInternalPort(8080);
        template.setCpuLimit(1.0);
        template.setMemoryLimit(512);
        template.setVolumeMountPath("/app/data");
        template.setNetworkMode("bridge");
        template.setRestartPolicy("unless-stopped");
        template.setEnabled(true);
        template.setIsDefault(true);
        template.setCreatedBy("SYSTEM");
        template.setSortOrder(0);
        return template;
    }

    /** 模板统计信息内部类 */
    public static class TemplateStatistics {
        private final long totalTemplates;
        private final long enabledTemplates;

        public TemplateStatistics(long totalTemplates, long enabledTemplates) {
            this.totalTemplates = totalTemplates;
            this.enabledTemplates = enabledTemplates;
        }

        public long getTotalTemplates() {
            return totalTemplates;
        }

        public long getEnabledTemplates() {
            return enabledTemplates;
        }
    }
}