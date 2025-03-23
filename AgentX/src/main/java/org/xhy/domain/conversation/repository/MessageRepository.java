package org.xhy.domain.conversation.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.conversation.model.Message;

/**
 * 消息仓库接口
 */
@Mapper
public interface MessageRepository extends BaseMapper<Message> {
}