package org.xhy.domain.trace.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.trace.model.AgentExecutionSummaryEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** Agent执行链路汇总仓库接口 */
@Mapper
public interface AgentExecutionSummaryRepository extends MyBatisPlusExtRepository<AgentExecutionSummaryEntity> {
}