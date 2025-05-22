package org.xhy.domain.tool.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** 工具仓储接口 */
@Mapper
public interface ToolRepository extends MyBatisPlusExtRepository<ToolEntity> {
}