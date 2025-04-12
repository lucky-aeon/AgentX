package org.xhy.domain.rag.repository;

/**
 * @author zang
 * @date 18:01 <br/>
 */

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.rag.model.DocumentChunkEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/**
 * 文档块仓储接口
 * @author zang
 */
@Mapper
public interface DocumentChunkRepository extends MyBatisPlusExtRepository<ModelEntity> {

}
