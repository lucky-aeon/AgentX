package org.xhy.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import org.xhy.domain.container.model.ContainerTemplateEntity;
import org.xhy.domain.container.repository.ContainerTemplateRepository;

import java.util.List;

/** 容器模板仓储实现 */
@Mapper
@Repository
public interface ContainerTemplateRepositoryImpl extends ContainerTemplateRepository {

    @Override
    default List<ContainerTemplateEntity> findByType(String type) {
        LambdaQueryWrapper<ContainerTemplateEntity> wrapper = Wrappers.<ContainerTemplateEntity>lambdaQuery()
                .eq(ContainerTemplateEntity::getType, type)
                .eq(ContainerTemplateEntity::getEnabled, true)
                .orderByAsc(ContainerTemplateEntity::getSortOrder)
                .orderByDesc(ContainerTemplateEntity::getCreatedAt);
        
        return selectList(wrapper);
    }

    @Override
    default ContainerTemplateEntity findDefaultByType(String type) {
        LambdaQueryWrapper<ContainerTemplateEntity> wrapper = Wrappers.<ContainerTemplateEntity>lambdaQuery()
                .eq(ContainerTemplateEntity::getType, type)
                .eq(ContainerTemplateEntity::getEnabled, true)
                .eq(ContainerTemplateEntity::getIsDefault, true)
                .orderByDesc(ContainerTemplateEntity::getCreatedAt);
        
        return selectOne(wrapper);
    }

    @Override
    default List<ContainerTemplateEntity> findEnabledTemplates() {
        LambdaQueryWrapper<ContainerTemplateEntity> wrapper = Wrappers.<ContainerTemplateEntity>lambdaQuery()
                .eq(ContainerTemplateEntity::getEnabled, true)
                .orderByAsc(ContainerTemplateEntity::getSortOrder)
                .orderByDesc(ContainerTemplateEntity::getCreatedAt);
        
        return selectList(wrapper);
    }

    @Override
    default ContainerTemplateEntity findByName(String name) {
        LambdaQueryWrapper<ContainerTemplateEntity> wrapper = Wrappers.<ContainerTemplateEntity>lambdaQuery()
                .eq(ContainerTemplateEntity::getName, name);
        
        return selectOne(wrapper);
    }

    @Override
    default boolean existsByName(String name, String excludeId) {
        LambdaQueryWrapper<ContainerTemplateEntity> wrapper = Wrappers.<ContainerTemplateEntity>lambdaQuery()
                .eq(ContainerTemplateEntity::getName, name);
        
        if (StringUtils.isNotBlank(excludeId)) {
            wrapper.ne(ContainerTemplateEntity::getId, excludeId);
        }
        
        return selectCount(wrapper) > 0;
    }

    @Override
    default Page<ContainerTemplateEntity> selectPageWithConditions(Page<ContainerTemplateEntity> page, 
                                                                  String keyword, 
                                                                  String type, 
                                                                  Boolean enabled) {
        LambdaQueryWrapper<ContainerTemplateEntity> wrapper = Wrappers.<ContainerTemplateEntity>lambdaQuery();
        
        // 关键词搜索：模板名称、描述、镜像名称
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(ContainerTemplateEntity::getName, keyword)
                    .or().like(ContainerTemplateEntity::getDescription, keyword)
                    .or().like(ContainerTemplateEntity::getImage, keyword));
        }
        
        // 类型过滤
        if (StringUtils.isNotBlank(type)) {
            wrapper.eq(ContainerTemplateEntity::getType, type);
        }
        
        // 启用状态过滤
        if (enabled != null) {
            wrapper.eq(ContainerTemplateEntity::getEnabled, enabled);
        }
        
        // 按排序权重和创建时间排序
        wrapper.orderByAsc(ContainerTemplateEntity::getSortOrder)
                .orderByDesc(ContainerTemplateEntity::getCreatedAt);
        
        return selectPage(page, wrapper);
    }

    @Override
    default List<ContainerTemplateEntity> findByCreatedBy(String createdBy) {
        LambdaQueryWrapper<ContainerTemplateEntity> wrapper = Wrappers.<ContainerTemplateEntity>lambdaQuery()
                .eq(ContainerTemplateEntity::getCreatedBy, createdBy)
                .orderByDesc(ContainerTemplateEntity::getCreatedAt);
        
        return selectList(wrapper);
    }

    @Override
    default long countTemplates() {
        return selectCount(null);
    }

    @Override
    default long countEnabledTemplates() {
        LambdaQueryWrapper<ContainerTemplateEntity> wrapper = Wrappers.<ContainerTemplateEntity>lambdaQuery()
                .eq(ContainerTemplateEntity::getEnabled, true);
        
        return selectCount(wrapper);
    }
}