package org.xhy.domain.tool.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.xhy.domain.tool.model.ToolVersionEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

import java.util.List;

@Mapper
public interface ToolVersionRepository extends MyBatisPlusExtRepository<ToolVersionEntity> {

    @Select("SELECT * FROM tool_version t1 " + "WHERE t1.public_status = true "
            + "AND t1.created_at = (SELECT MAX(t2.created_at) FROM tool_version t2 WHERE t2.tool_id = t1.tool_id AND t2.public_status = true)")
    List<ToolVersionEntity> listLatestPublicToolVersions();
}
