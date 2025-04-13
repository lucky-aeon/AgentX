package org.xhy.domain.plugins.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import org.springframework.stereotype.Service;
import org.xhy.domain.plugins.constant.PluginStatus;
import org.xhy.domain.plugins.model.PluginEntity;
import org.xhy.domain.plugins.repository.PluginRepository;

@Service
public class PluginBaseDomainService {
    protected final PluginRepository pluginRepository;

    public PluginBaseDomainService(PluginRepository pluginRepository) {
        this.pluginRepository = pluginRepository;
    }
    
    public PluginEntity getPluginDetail(String id) {
        return pluginRepository.selectById(id);
    }

    // 搜索插件
    public List<PluginEntity> searchPlugins(String name, PluginStatus status, Integer page, Integer size) {
        return pluginRepository.selectList(new LambdaQueryWrapper<PluginEntity>()
            .like(PluginEntity::getName, name)
            .eq(PluginEntity::getStatus, status)
            .orderByDesc(PluginEntity::getId)
            .last("limit " + (page - 1) * size + ", " + size));
    }
}
