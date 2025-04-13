package org.xhy.domain.plugins.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.plugins.model.PluginEntity;

@Mapper
public interface PluginRepository extends BaseMapper<PluginEntity> {
}
