package org.xhy.domain.llm.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.llm.model.ProviderEntity;


/**
 * 服务提供商仓储接口
 */
@Mapper
public interface ProviderRepository extends BaseMapper<ProviderEntity> {

}