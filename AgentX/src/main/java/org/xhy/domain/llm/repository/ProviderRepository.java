package org.xhy.domain.llm.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** 服务提供商仓储接口 */
@Mapper
public interface ProviderRepository extends MyBatisPlusExtRepository<ProviderEntity> {
}
