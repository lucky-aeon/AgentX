package org.xhy.domain.agent.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.xhy.domain.agent.model.AgentVersionEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

import java.util.List;

/** Agent版本仓库接口 */
@Mapper
public interface AgentVersionRepository extends MyBatisPlusExtRepository<AgentVersionEntity> {

    /** 查询每个agentId的最新版本（按publishStatus过滤）
     * 
     * @param publishStatus 发布状态，为null时查询所有状态
     * @return 每个agentId的最新版本列表 */
    @Select("<script>" + "SELECT a.* FROM agent_versions a "
            + "INNER JOIN (SELECT agent_id, MAX(published_at) as max_published_at " + "FROM agent_versions "
            + "<if test='publishStatus != null'> WHERE publish_status = #{publishStatus} </if>"
            + "GROUP BY agent_id) b " + "ON a.agent_id = b.agent_id AND a.published_at = b.max_published_at "
            + "<if test='publishStatus != null'> WHERE a.publish_status = #{publishStatus} </if>" + "</script>")
    List<AgentVersionEntity> selectLatestVersionsByStatus(Integer publishStatus);

    /** 按名称搜索每个agentId的最新版本
     * 
     * @param name 搜索的名称，模糊匹配
     * @return 符合条件的每个agentId的最新版本列表 */
    @Select("<script>" + "SELECT a.* FROM agent_versions a "
            + "INNER JOIN (SELECT agent_id, MAX(published_at) as max_published_at " + "FROM agent_versions "
            + "<if test='name != null and name != \"\"'> WHERE name LIKE CONCAT('%', #{name}, '%') </if>"
            + "GROUP BY agent_id) b " + "ON a.agent_id = b.agent_id AND a.published_at = b.max_published_at "
            + "<if test='name != null and name != \"\"'> WHERE a.name LIKE CONCAT('%', #{name}, '%') </if>"
            + "</script>")
    List<AgentVersionEntity> selectLatestVersionsByName(String name);

    /** 根据名称和发布状态查询所有助理的最新版本 同时支持只按状态查询（当name为空时） */
    @Select({"<script>", "SELECT v.* FROM agent_versions v ", "INNER JOIN (",
            "    SELECT agent_id, MAX(published_at) as latest_date ", "    FROM agent_versions ",
            "    WHERE deleted_at IS NULL ", "    <if test='status != null'>",
            "        AND publish_status = #{status} ", "    </if>", "    GROUP BY agent_id",
            ") latest ON v.agent_id = latest.agent_id AND v.published_at = latest.latest_date ",
            "WHERE v.deleted_at IS NULL ", "<if test='name != null and name != \"\"'>",
            "    AND v.name LIKE CONCAT('%', #{name}, '%') ", "</if>", "<if test='status != null'>",
            "    AND v.publish_status = #{status} ", "</if>", "</script>"})
    List<AgentVersionEntity> selectLatestVersionsByNameAndStatus(String name, Integer status);
}