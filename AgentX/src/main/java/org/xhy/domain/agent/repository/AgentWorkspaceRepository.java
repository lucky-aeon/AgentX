package org.xhy.domain.agent.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.xhy.application.agent.service.AgentWorkspaceAppService;
import org.xhy.domain.agent.model.AgentWorkspaceEntity;

/**
 * Agent工作区仓库接口
 */
@Mapper
public interface AgentWorkspaceRepository extends BaseMapper<AgentWorkspaceEntity> {

    @Select("SELECT EXISTS(SELECT 1 FROM agent_workspace WHERE agent_id = #{agentId} AND user_id = #{userId})")
    boolean checkAgentWorkspaceExist(@Param("agentId") String agentId, @Param("userId") String userId);
}
