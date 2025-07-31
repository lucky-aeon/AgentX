package org.xhy.domain.rag.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.rag.model.UserRagDocumentEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** 用户RAG文档快照仓储接口
 * @author xhy
 * @date 2025-07-22 <br/>
 */
@Mapper
public interface UserRagDocumentRepository extends MyBatisPlusExtRepository<UserRagDocumentEntity> {

}