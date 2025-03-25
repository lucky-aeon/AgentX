package org.xhy.domain.conversation.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.conversation.model.SessionEntity;

/**
 * 会话仓库接口
 */
@Mapper
public interface SessionRepository extends BaseMapper<SessionEntity> {
    // 使用MyBatis Plus的方式实现查询，不再使用手写SQL
}