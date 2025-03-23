package org.xhy.domain.conversation.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.conversation.model.Session;

/**
 * 会话仓库接口
 */
@Mapper
public interface SessionRepository extends BaseMapper<Session> {
}