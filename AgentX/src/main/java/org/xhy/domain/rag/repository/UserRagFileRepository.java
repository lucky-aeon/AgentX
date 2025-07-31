package org.xhy.domain.rag.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.rag.model.UserRagFileEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** 用户RAG文件快照仓储接口
 * @author xhy
 * @date 2025-07-22 <br/>
 */
@Mapper
public interface UserRagFileRepository extends MyBatisPlusExtRepository<UserRagFileEntity> {

}