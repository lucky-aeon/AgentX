package org.xhy.domain.user.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.user.model.UsageRecordEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** 用量记录仓储接口 */
@Mapper
public interface UsageRecordRepository extends MyBatisPlusExtRepository<UsageRecordEntity> {
}