package org.xhy.domain.embedding.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.embedding.model.DocumentUnitEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/**
 * @author shilong.zang
 * @date 21:07 <br/>
 */
@Mapper
public interface DocumentUnitRepository extends MyBatisPlusExtRepository<DocumentUnitEntity> {

}
