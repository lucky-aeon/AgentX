package org.xhy.domain.conversation.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.conversation.model.SessionEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** 会话仓库接口 */
@Mapper
public interface SessionRepository extends MyBatisPlusExtRepository<SessionEntity> {
}
