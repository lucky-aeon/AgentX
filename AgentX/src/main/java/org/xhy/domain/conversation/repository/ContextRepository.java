package org.xhy.domain.conversation.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.conversation.model.ContextEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/**
 * 上下文仓库接口
 */
@Mapper
public interface ContextRepository extends MyBatisPlusExtRepository<ContextEntity> {
}