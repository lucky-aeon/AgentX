package org.xhy.domain.apikey.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.apikey.model.ApiKeyEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** API密钥仓储接口 */
@Mapper
public interface ApiKeyRepository extends MyBatisPlusExtRepository<ApiKeyEntity> {
}