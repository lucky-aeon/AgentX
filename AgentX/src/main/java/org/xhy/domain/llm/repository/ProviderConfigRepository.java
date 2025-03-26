package org.xhy.domain.llm.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.llm.model.ProviderConfig;
import java.util.List;

@Mapper
public interface ProviderConfigRepository extends BaseMapper<ProviderConfig> {

} 