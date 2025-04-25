package org.xhy.domain.rag.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/**
 * @author shilong.zang
 * @date 21:07 <br/>
 */
@Mapper
public interface DocumentUnitRepository extends MyBatisPlusExtRepository<DocumentUnitEntity> {

}
