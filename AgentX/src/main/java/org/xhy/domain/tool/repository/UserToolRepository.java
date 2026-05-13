package org.xhy.domain.tool.repository;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.xhy.domain.tool.model.UserToolEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

@Mapper
public interface UserToolRepository extends MyBatisPlusExtRepository<UserToolEntity> {

    @Delete("""
            DELETE FROM user_tools
            WHERE user_id = #{userId}
              AND tool_id = #{toolId}
            """)
    int physicalDeleteByUserIdAndToolId(@Param("userId") String userId, @Param("toolId") String toolId);
}
