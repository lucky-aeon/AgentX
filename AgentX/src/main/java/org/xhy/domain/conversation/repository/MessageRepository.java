package org.xhy.domain.conversation.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** 消息仓库接口 */
@Mapper
public interface MessageRepository extends MyBatisPlusExtRepository<MessageEntity> {
}
