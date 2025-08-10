package org.xhy.domain.rule.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.rule.model.RuleEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** 规则仓储接口 */
@Mapper
public interface RuleRepository extends MyBatisPlusExtRepository<RuleEntity> {
}