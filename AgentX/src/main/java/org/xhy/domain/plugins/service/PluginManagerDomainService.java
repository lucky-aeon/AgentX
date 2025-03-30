package org.xhy.domain.plugins.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.util.StringUtils;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.domain.plugins.constant.PluginStatus;
import org.xhy.domain.plugins.constant.PluginType;
import org.xhy.domain.plugins.model.GitRepository;
import org.xhy.domain.plugins.model.License;
import org.xhy.domain.plugins.config.PluginConfig;
import org.xhy.domain.plugins.config.StdioPluginConfig;
import org.xhy.domain.plugins.model.PluginEntity;
import org.xhy.domain.plugins.repository.PluginRepository;
import org.xhy.infrastructure.exception.BusinessException;

@Service
public class PluginManagerDomainService extends PluginBaseDomainService {

    public PluginManagerDomainService(PluginRepository pluginRepository) {
        super(pluginRepository);
    }

    /**
     * 创建插件申请
     */
    @Transactional
    public PluginEntity submitPlugin(String name, String description, String repositoryUrl, String license, 
            PluginConfig config) {
        // 转换为值对象
        GitRepository repository = GitRepository.from(repositoryUrl);
        License licenseObj = License.from(license);

        // 检查仓库URL是否已存在
        if (existsByRepositoryUrl(repository.getUrl())) {
            throw new BusinessException("该仓库已经提交过申请");
        }

        // 验证配置
        if (!config.validate()) {
            throw new BusinessException("插件配置无效");
        }

        // 验证类型和配置是否匹配
        if (config.validate()) {
            throw new BusinessException("SSE插件必须提供SSE配置");
        }
        if (config.getConfigType().equals(PluginType.STDIO.getName()) && !(config instanceof StdioPluginConfig)) {
            throw new BusinessException("STDIO插件必须提供STDIO配置");
        }

        // 创建插件实体
        PluginEntity plugin = PluginEntity.create(name, description, repository, licenseObj);
        plugin.setConfig(config);
        plugin.setStatus(PluginStatus.PENDING);
        pluginRepository.insert(plugin);
        return plugin;
    }

    /**
     * 检查仓库URL是否已存在
     */
    private boolean existsByRepositoryUrl(String repositoryUrl) {
        LambdaQueryWrapper<PluginEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PluginEntity::getRepositoryUrl, repositoryUrl);
        return pluginRepository.selectCount(wrapper) > 0;
    }

    /**
     * 更新插件状态
     */
    @Transactional
    public void updateStatus(String id, PluginStatus status) {
        PluginEntity plugin = pluginRepository.selectById(id);
        if (plugin == null) {
            throw new BusinessException("插件不存在");
        }

        plugin.updateStatus(status);
        pluginRepository.updateById(plugin);
    }

    /**
     * 获取插件列表, userId 为空时，允许获取所有查询结果
     * 
     * @param userId 用户ID
     * @param name 插件名称，模糊匹配
     * @param status 插件状态
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 插件列表
     */
    public List<PluginEntity> listPlugins(String userId, String name, PluginStatus status, Integer page, Integer size) {
        LambdaQueryWrapper<PluginEntity> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加名称模糊查询条件
        if (StringUtils.hasText(name)) {
            queryWrapper.like(PluginEntity::getName, name);
        }
        
        // 添加状态查询条件
        if (status != null) {
            queryWrapper.eq(PluginEntity::getStatus, status);
        }
        
        // 添加用户ID查询条件
        if (userId != null) {
            queryWrapper.eq(PluginEntity::getUserId, userId);
        }
        
        // 添加排序条件：按ID降序
        queryWrapper.orderByDesc(PluginEntity::getId);
        
        // 处理分页
        if (page != null && size != null) {
            Page<PluginEntity> pageParam = new Page<>(page, size);
            return pluginRepository.selectPage(pageParam, queryWrapper).getRecords();
        }
        
        return pluginRepository.selectList(queryWrapper);
    }

    /**
     * 删除插件
     */
    public void deletePlugin(String id) {
        pluginRepository.deleteById(id);
    }

    /**
     * 更新插件
     */
    @Transactional
    public PluginEntity updatePlugin(String id, String name, String description, String repositoryUrl, 
            String license, PluginConfig config) {
        PluginEntity plugin = getPluginById(id);
        if (plugin == null) {
            throw new BusinessException("插件不存在");
        }

        // 转换为值对象
        GitRepository repository = GitRepository.from(repositoryUrl);
        License licenseObj = License.from(license);

        // 检查仓库URL是否已被其他插件使用
        if (!plugin.getRepositoryUrl().equals(repository.getUrl()) && existsByRepositoryUrl(repository.getUrl())) {
            throw new BusinessException("该仓库已经被其他插件使用");
        }

        // 验证配置
        config.validate();

        // 更新插件
        plugin.setName(name);
        plugin.setDescription(description);
        plugin.setRepositoryUrl(repository.getUrl());
        plugin.setLicense(licenseObj.getName());
        plugin.setConfig(config);
        plugin.setStatus(PluginStatus.PENDING);
        pluginRepository.updateById(plugin);
        return plugin;
    }

    private PluginEntity getPluginById(String id) {
        return pluginRepository.selectById(id);
    }
}
