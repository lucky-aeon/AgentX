package org.xhy.domain.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.xhy.domain.auth.constant.AuthFeatureKey;
import org.xhy.domain.auth.constant.FeatureType;
import org.xhy.domain.auth.model.AuthSettingEntity;
import org.xhy.domain.auth.repository.AuthSettingRepository;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.List;

/** 认证配置领域服务 */
@Service
public class AuthSettingDomainService {

    private final AuthSettingRepository authSettingRepository;

    public AuthSettingDomainService(AuthSettingRepository authSettingRepository) {
        this.authSettingRepository = authSettingRepository;
    }

    /** 获取指定类型的启用功能列表
     * 
     * @param featureType 功能类型
     * @return 启用功能列表 */
    public List<AuthSettingEntity> getEnabledFeatures(FeatureType featureType) {
        LambdaQueryWrapper<AuthSettingEntity> wrapper = Wrappers.<AuthSettingEntity>lambdaQuery()
                .eq(AuthSettingEntity::getFeatureType, featureType.getCode()).eq(AuthSettingEntity::getEnabled, true)
                .orderByAsc(AuthSettingEntity::getDisplayOrder);

        return authSettingRepository.selectList(wrapper);
    }

    /** 获取指定类型的所有功能列表
     * 
     * @param featureType 功能类型
     * @return 功能列表 */
    public List<AuthSettingEntity> getAllFeatures(FeatureType featureType) {
        LambdaQueryWrapper<AuthSettingEntity> wrapper = Wrappers.<AuthSettingEntity>lambdaQuery()
                .eq(AuthSettingEntity::getFeatureType, featureType.getCode())
                .orderByAsc(AuthSettingEntity::getDisplayOrder);

        return authSettingRepository.selectList(wrapper);
    }

    /** 获取所有认证配置
     * 
     * @return 所有认证配置列表 */
    public List<AuthSettingEntity> getAllAuthSettings() {
        LambdaQueryWrapper<AuthSettingEntity> wrapper = Wrappers.<AuthSettingEntity>lambdaQuery()
                .orderByAsc(AuthSettingEntity::getFeatureType, AuthSettingEntity::getDisplayOrder);

        return authSettingRepository.selectList(wrapper);
    }

    /** 检查指定功能是否启用
     * 
     * @param featureKey 功能键
     * @return 是否启用 */
    public boolean isFeatureEnabled(AuthFeatureKey featureKey) {
        LambdaQueryWrapper<AuthSettingEntity> wrapper = Wrappers.<AuthSettingEntity>lambdaQuery()
                .eq(AuthSettingEntity::getFeatureKey, featureKey.getCode()).eq(AuthSettingEntity::getEnabled, true);

        return authSettingRepository.selectCount(wrapper) > 0;
    }

    /** 根据功能键获取认证配置
     * 
     * @param featureKey 功能键
     * @return 认证配置实体 */
    public AuthSettingEntity getByFeatureKey(AuthFeatureKey featureKey) {
        LambdaQueryWrapper<AuthSettingEntity> wrapper = Wrappers.<AuthSettingEntity>lambdaQuery()
                .eq(AuthSettingEntity::getFeatureKey, featureKey.getCode());

        return authSettingRepository.selectOne(wrapper);
    }

    /** 根据ID获取认证配置
     * 
     * @param id 配置ID
     * @return 认证配置实体 */
    public AuthSettingEntity getById(String id) {
        AuthSettingEntity entity = authSettingRepository.selectById(id);
        if (entity == null) {
            throw new BusinessException("认证配置不存在");
        }
        return entity;
    }

    /** 切换功能启用状态
     * 
     * @param id 配置ID
     * @return 更新后的配置 */
    public AuthSettingEntity toggleEnabled(String id) {
        AuthSettingEntity entity = getById(id);

        LambdaUpdateWrapper<AuthSettingEntity> updateWrapper = Wrappers.<AuthSettingEntity>lambdaUpdate()
                .eq(AuthSettingEntity::getId, id).set(AuthSettingEntity::getEnabled, !entity.getEnabled());

        authSettingRepository.checkedUpdate(null, updateWrapper);

        // 返回更新后的实体
        entity.setEnabled(!entity.getEnabled());
        return entity;
    }

    /** 更新认证配置
     * 
     * @param entity 认证配置实体
     * @return 更新后的配置 */
    public AuthSettingEntity updateAuthSetting(AuthSettingEntity entity) {
        AuthSettingEntity existingEntity = getById(entity.getId());

        LambdaUpdateWrapper<AuthSettingEntity> updateWrapper = Wrappers.<AuthSettingEntity>lambdaUpdate()
                .eq(AuthSettingEntity::getId, entity.getId());

        authSettingRepository.checkedUpdate(entity, updateWrapper);

        return entity;
    }

    /** 创建认证配置
     * 
     * @param entity 认证配置实体
     * @return 创建的配置 */
    public AuthSettingEntity createAuthSetting(AuthSettingEntity entity) {
        // 检查功能键是否已存在
        LambdaQueryWrapper<AuthSettingEntity> wrapper = Wrappers.<AuthSettingEntity>lambdaQuery()
                .eq(AuthSettingEntity::getFeatureKey, entity.getFeatureKey());

        if (authSettingRepository.selectCount(wrapper) > 0) {
            throw new BusinessException("功能键已存在: " + entity.getFeatureKey());
        }

        authSettingRepository.checkInsert(entity);
        return entity;
    }

    /** 删除认证配置
     * 
     * @param id 配置ID */
    public void deleteAuthSetting(String id) {
        AuthSettingEntity entity = getById(id);
        authSettingRepository.deleteById(id);
    }
}