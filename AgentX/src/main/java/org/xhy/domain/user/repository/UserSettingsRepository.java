package org.xhy.domain.user.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.user.model.UserSettingsEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** 用户设置仓储接口 */
@Mapper
public interface UserSettingsRepository extends MyBatisPlusExtRepository<UserSettingsEntity> {

} 