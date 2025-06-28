package org.xhy.domain.container.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.xhy.domain.container.model.ContainerTemplateEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

import java.util.List;

/** 容器模板仓储接口 */
public interface ContainerTemplateRepository extends MyBatisPlusExtRepository<ContainerTemplateEntity> {

    /** 根据模板类型查找模板
     * 
     * @param type 模板类型
     * @return 模板列表 */
    List<ContainerTemplateEntity> findByType(String type);

    /** 查找默认模板
     * 
     * @param type 模板类型
     * @return 默认模板，可能为null */
    ContainerTemplateEntity findDefaultByType(String type);

    /** 查找所有启用的模板
     * 
     * @return 启用的模板列表 */
    List<ContainerTemplateEntity> findEnabledTemplates();

    /** 根据名称查找模板
     * 
     * @param name 模板名称
     * @return 模板，可能为null */
    ContainerTemplateEntity findByName(String name);

    /** 检查模板名称是否已存在
     * 
     * @param name 模板名称
     * @param excludeId 排除的模板ID
     * @return 是否存在 */
    boolean existsByName(String name, String excludeId);

    /** 分页查询模板
     * 
     * @param page 分页参数
     * @param keyword 搜索关键词
     * @param type 模板类型
     * @param enabled 是否启用
     * @return 分页结果 */
    Page<ContainerTemplateEntity> selectPageWithConditions(Page<ContainerTemplateEntity> page, String keyword,
            String type, Boolean enabled);

    /** 根据创建者查找模板
     * 
     * @param createdBy 创建者用户ID
     * @return 模板列表 */
    List<ContainerTemplateEntity> findByCreatedBy(String createdBy);

    /** 获取模板总数
     * 
     * @return 模板总数 */
    long countTemplates();

    /** 获取启用模板数量
     * 
     * @return 启用模板数量 */
    long countEnabledTemplates();
}