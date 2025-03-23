package org.xhy.domain.conversation.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.conversation.model.Context;

/**
 * 上下文仓库接口
 */
@Mapper
public interface ContextRepository extends BaseMapper<Context> {
}