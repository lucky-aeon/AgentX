package org.xhy.domain.tool.service;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.tool.model.ToolVersionEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

@Mapper
public interface ToolVersionRepository extends MyBatisPlusExtRepository<ToolVersionEntity> {

}
