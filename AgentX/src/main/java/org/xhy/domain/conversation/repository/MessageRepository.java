package org.xhy.domain.conversation.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.xhy.domain.conversation.model.Message;

import java.util.List;

/**
 * 消息仓库接口
 */
@Mapper
public interface MessageRepository extends BaseMapper<Message> {

    /**
     * 根据会话ID查询消息列表
     */
    @Select("SELECT * FROM messages WHERE session_id = #{sessionId} ORDER BY created_at")
    List<Message> findBySessionIdOrderByCreatedAt(@Param("sessionId") String sessionId);

    /**
     * 根据会话ID和角色查询消息列表
     */
    @Select("SELECT * FROM messages WHERE session_id = #{sessionId} AND role = #{role} ORDER BY created_at")
    List<Message> findBySessionIdAndRoleOrderByCreatedAt(@Param("sessionId") String sessionId,
            @Param("role") String role);

    /**
     * 根据会话ID查询最新的N条消息
     */
    @Select("SELECT * FROM messages WHERE session_id = #{sessionId} ORDER BY created_at DESC LIMIT #{n}")
    List<Message> findTopNBySessionIdOrderByCreatedAtDesc(@Param("sessionId") String sessionId, @Param("n") int n);

    /**
     * 根据会话ID批量删除消息
     */
    @Delete("DELETE FROM messages WHERE session_id = #{sessionId}")
    void deleteBySessionId(@Param("sessionId") String sessionId);
}