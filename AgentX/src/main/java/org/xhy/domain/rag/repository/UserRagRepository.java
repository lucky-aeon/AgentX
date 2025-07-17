package org.xhy.domain.rag.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.rag.model.UserRagEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** 用户安装的RAG仓储接口
 * @author xhy
 * @date 2025-07-16 <br/>
 */
@Mapper
public interface UserRagRepository extends MyBatisPlusExtRepository<UserRagEntity> {

}