package org.xhy.domain.conversation.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.xhy.domain.conversation.model.Context;

/**
 * 上下文仓库接口
 */
@Mapper
public interface ContextRepository extends BaseMapper<Context> {

    /**
     * 根据会话ID查询上下文
     */
    @Select("SELECT * FROM context WHERE session_id = #{sessionId} LIMIT 1")
    Context findBySessionId(@Param("sessionId") String sessionId);

    /**
     * 根据会话ID删除上下文
     */
    @Delete("DELETE FROM context WHERE session_id = #{sessionId}")
    void deleteBySessionId(@Param("sessionId") String sessionId);
}