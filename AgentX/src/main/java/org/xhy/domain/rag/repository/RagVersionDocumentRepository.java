package org.xhy.domain.rag.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.rag.model.RagVersionDocumentEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** RAG版本文档单元仓储接口
 * @author xhy
 * @date 2025-07-16 <br/>
 */
@Mapper
public interface RagVersionDocumentRepository extends MyBatisPlusExtRepository<RagVersionDocumentEntity> {

}