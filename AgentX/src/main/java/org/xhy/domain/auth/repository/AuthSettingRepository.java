package org.xhy.domain.auth.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.auth.model.AuthSettingEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** 认证配置Repository接口 */
@Mapper
public interface AuthSettingRepository extends MyBatisPlusExtRepository<AuthSettingEntity> {
}