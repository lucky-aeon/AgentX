package org.xhy.domain.conversation.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.xhy.domain.conversation.model.Session;

import java.util.List;

/**
 * 会话仓库接口
 */
@Mapper
public interface SessionRepository extends BaseMapper<Session> {

        /**
         * 根据用户ID查询会话列表
         */
        @Select("SELECT * FROM sessions WHERE user_id = #{userId} ORDER BY updated_at DESC")
        List<Session> findByUserIdOrderByUpdatedAtDesc(@Param("userId") String userId);

        /**
         * 根据用户ID和归档状态查询会话列表
         */
        @Select("SELECT * FROM sessions WHERE user_id = #{userId} AND is_archived = #{isArchived} ORDER BY updated_at DESC")
        List<Session> findByUserIdAndIsArchivedOrderByUpdatedAtDesc(@Param("userId") String userId,
                        @Param("isArchived") boolean isArchived);

        /**
         * 根据标题模糊查询
         */
        @Select("SELECT * FROM sessions WHERE user_id = #{userId} AND title LIKE CONCAT('%', #{titleKeyword}, '%') ORDER BY updated_at DESC")
        List<Session> findByUserIdAndTitleContainingOrderByUpdatedAtDesc(@Param("userId") String userId,
                        @Param("titleKeyword") String titleKeyword);
}