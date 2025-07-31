package org.xhy.domain.rag.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.rag.model.RagVersionFileEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** RAG版本文件仓储接口
 * @author xhy
 * @date 2025-07-16 <br/>
 */
@Mapper
public interface RagVersionFileRepository extends MyBatisPlusExtRepository<RagVersionFileEntity> {

}