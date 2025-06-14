package org.xhy.domain.llm.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.domain.llm.event.ModelCreatedEvent;
import org.xhy.domain.llm.event.ModelDeletedEvent;
import org.xhy.domain.llm.event.ModelStatusChangedEvent;
import org.xhy.domain.llm.event.ModelUpdatedEvent;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.model.ProviderAggregate;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.infrastructure.entity.Operator;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;
import org.xhy.domain.llm.model.enums.ProviderType;
import org.xhy.domain.llm.repository.ModelRepository;
import org.xhy.domain.llm.repository.ProviderRepository;
import org.springframework.stereotype.Service;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.domain.llm.event.ModelsBatchDeletedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** LLM领域服务 负责服务提供商和模型的核心业务逻辑 */
@Service
public class LLMDomainService {

    private final ProviderRepository providerRepository;
    private final ModelRepository modelRepository;
    private final ApplicationEventPublisher eventPublisher;

    public LLMDomainService(ProviderRepository providerRepository, ModelRepository modelRepository,
            ApplicationEventPublisher eventPublisher) {
        this.providerRepository = providerRepository;
        this.modelRepository = modelRepository;
        this.eventPublisher = eventPublisher;
    }

    /** 创建服务商
     * @param provider 服务商信息
     * @return 创建后的服务商ID */
    public ProviderEntity createProvider(ProviderEntity provider) {
        validateProviderProtocol(provider.getProtocol());
        providerRepository.insert(provider);
        return provider;
    }

    /** 更新服务商
     * @param provider 服务商信息 */
    public void updateProvider(ProviderEntity provider) {
        validateProviderProtocol(provider.getProtocol());
        LambdaUpdateWrapper<ProviderEntity> wrapper = Wrappers.<ProviderEntity>lambdaUpdate()
                .eq(ProviderEntity::getId, provider.getId())
                .eq(provider.needCheckUserId(), ProviderEntity::getUserId, provider.getUserId());
        providerRepository.checkedUpdate(provider, wrapper);
    }

    /** 获取用户自己的服务商
     * @param userId 用户id */
    public List<ProviderAggregate> getUserProviders(String userId) {
        Wrapper<ProviderEntity> wrapper = Wrappers.<ProviderEntity>lambdaQuery().eq(ProviderEntity::getUserId, userId);
        List<ProviderEntity> providers = providerRepository.selectList(wrapper);

        return buildProviderAggregatesWithActiveModels(providers);
    }

    /** 获取所有服务商（包含官方和用户自定义）
     * @param userId 用户ID
     * @return 服务商聚合根列表 */
    public List<ProviderAggregate> getAllProviders(String userId) {
        Wrapper<ProviderEntity> wrapper = Wrappers.<ProviderEntity>lambdaQuery().eq(ProviderEntity::getUserId, userId)
                .or().eq(ProviderEntity::getIsOfficial, true);
        List<ProviderEntity> providers = providerRepository.selectList(wrapper);

        return buildProviderAggregatesWithActiveModels(providers);
    }

    /** 获取官方服务商
     * @return 官方服务商聚合根列表 */
    public List<ProviderAggregate> getOfficialProviders() {
        Wrapper<ProviderEntity> wrapper = Wrappers.<ProviderEntity>lambdaQuery().eq(ProviderEntity::getIsOfficial,
                true);
        List<ProviderEntity> providers = providerRepository.selectList(wrapper);

        return buildProviderAggregatesWithActiveModels(providers);
    }

    /** 获取用户自定义服务商
     * @param userId 用户ID
     * @return 用户自定义服务商聚合根列表 */
    public List<ProviderAggregate> getCustomProviders(String userId) {
        Wrapper<ProviderEntity> wrapper = Wrappers.<ProviderEntity>lambdaQuery().eq(ProviderEntity::getUserId, userId)
                .eq(ProviderEntity::getIsOfficial, false);
        List<ProviderEntity> providers = providerRepository.selectList(wrapper);

        return buildProviderAggregatesWithActiveModels(providers);
    }

    /** 构建服务商聚合根，只包含激活的模型
     * @param providers 服务商列表
     * @return 服务商聚合根列表 */
    private List<ProviderAggregate> buildProviderAggregatesWithActiveModels(List<ProviderEntity> providers) {
        List<ProviderAggregate> providerAggregates = new ArrayList<>();
        if (providers == null || providers.isEmpty()) {
            return providerAggregates;
        }

        // 收集服务商id
        List<String> providerIds = providers.stream().map(ProviderEntity::getId).collect(Collectors.toList());
        // 查询激活的模型
        List<ModelEntity> activeModels = modelRepository.selectList(Wrappers.<ModelEntity>lambdaQuery()
                .in(ModelEntity::getProviderId, providerIds).eq(ModelEntity::getStatus, true));
        // 转为map，映射关系
        Map<String, List<ModelEntity>> modelMap = activeModels.stream()
                .collect(Collectors.groupingBy(ModelEntity::getProviderId));

        // 遍历服务商，创建聚合根，设置模型
        for (ProviderEntity provider : providers) {
            List<ModelEntity> modelList = modelMap.get(provider.getId());
            ProviderAggregate providerAggregate = new ProviderAggregate(provider, modelList);
            providerAggregates.add(providerAggregate);
        }
        return providerAggregates;
    }

    /** 获取服务商
     * @param providerId 服务商id
     * @param userId 用户id */
    public ProviderEntity getProvider(String providerId, String userId) {

        Wrapper<ProviderEntity> wrapper = Wrappers.<ProviderEntity>lambdaQuery().eq(ProviderEntity::getId, providerId)
                .eq(ProviderEntity::getUserId, userId);
        ProviderEntity provider = providerRepository.selectOne(wrapper);
        if (provider == null) {
            throw new BusinessException("服务商不存在");
        }
        return provider;
    }

    public ProviderEntity getProvider(String providerId) {

        Wrapper<ProviderEntity> wrapper = Wrappers.<ProviderEntity>lambdaQuery().eq(ProviderEntity::getId, providerId);
        ProviderEntity provider = providerRepository.selectOne(wrapper);
        if (provider == null) {
            throw new BusinessException("服务商不存在");
        }
        return provider;
    }

    /** 查找服务商
     * @param providerId 服务商id */
    public ProviderEntity findProviderById(String providerId) {
        Wrapper<ProviderEntity> wrapper = Wrappers.<ProviderEntity>lambdaQuery().eq(ProviderEntity::getId, providerId);
        ProviderEntity provider = providerRepository.selectById(wrapper);
        if (provider == null) {
            return null;
        }
        return provider;
    }

    // 检查服务商是否存在
    public void checkProviderExists(String providerId, String userId) {
        Wrapper<ProviderEntity> wrapper = Wrappers.<ProviderEntity>lambdaQuery().eq(ProviderEntity::getId, providerId)
                .eq(ProviderEntity::getUserId, userId);
        ProviderEntity provider = providerRepository.selectOne(wrapper);
        if (provider == null) {
            throw new BusinessException("服务商不存在");
        }
    }

    // 获取服务商聚合根
    public ProviderAggregate getProviderAggregate(String providerId, String userId) {
        // 获取服务商
        ProviderEntity provider = getProvider(providerId, userId);
        // 获取服务商下的激活模型列表
        List<ModelEntity> modelList = getActiveModelList(providerId, userId);

        return new ProviderAggregate(provider, modelList);
    }

    // 获取模型列表
    public List<ModelEntity> getModelList(String providerId, String userId) {
        Wrapper<ModelEntity> wrapper = Wrappers.<ModelEntity>lambdaQuery().eq(ModelEntity::getProviderId, providerId)
                .eq(ModelEntity::getUserId, userId);
        return modelRepository.selectList(wrapper);
    }

    /** 获取激活的模型列表
     * @param providerId 服务商ID
     * @param userId 用户ID
     * @return 激活的模型列表 */
    public List<ModelEntity> getActiveModelList(String providerId, String userId) {
        Wrapper<ModelEntity> wrapper = Wrappers.<ModelEntity>lambdaQuery().eq(ModelEntity::getProviderId, providerId)
                .eq(ModelEntity::getUserId, userId).eq(ModelEntity::getStatus, true);
        return modelRepository.selectList(wrapper);
    }

    /** 删除服务商
     * @param providerId 服务商id
     * @param userId 用户id */
    @Transactional
    public void deleteProvider(String providerId, String userId, Operator operator) {
        // 删除服务商前先获取要删除的模型列表，用于发布批量删除事件
        Wrapper<ModelEntity> modelQueryWrapper = Wrappers.<ModelEntity>lambdaQuery().eq(ModelEntity::getProviderId,
                providerId);
        List<ModelEntity> modelsToDelete = modelRepository.selectList(modelQueryWrapper);

        Wrapper<ProviderEntity> wrapper = Wrappers.<ProviderEntity>lambdaQuery().eq(ProviderEntity::getId, providerId)
                .eq(operator.needCheckUserId(), ProviderEntity::getUserId, userId);
        providerRepository.checkedDelete(wrapper);

        // 删除模型
        Wrapper<ModelEntity> modelWrapper = Wrappers.<ModelEntity>lambdaQuery().eq(ModelEntity::getProviderId,
                providerId);
        int delete = modelRepository.delete(modelWrapper);

        // 如果有模型被删除，发布批量删除事件
        if (delete > 0) {
            List<ModelsBatchDeletedEvent.ModelDeleteItem> deleteItems = modelsToDelete.stream()
                    .map(model -> new ModelsBatchDeletedEvent.ModelDeleteItem(model.getId(), model.getUserId()))
                    .collect(Collectors.toList());

            eventPublisher.publishEvent(new ModelsBatchDeletedEvent(deleteItems, userId));
        }
    }

    /** 验证服务商协议是否支持
     * @param protocol 协议 */
    private void validateProviderProtocol(ProviderProtocol protocol) {
        // TODO: 从配置或枚举中获取支持的服务商协议列表
        if (!isSupportedProvider(protocol)) {
            throw new BusinessException("不支持的服务商协议类型: " + protocol);
        }
    }

    /** 检查是否是支持的服务商协议
     * @param protocol 服务商提供商编码
     * @return */
    private boolean isSupportedProvider(ProviderProtocol protocol) {
        return Arrays.stream(ProviderProtocol.values()).anyMatch(providerType -> providerType == protocol);
    }

    /** 获取所有支持的服务商协议
     * @return */
    public List<ProviderProtocol> getProviderProtocols() {
        return Arrays.asList(ProviderProtocol.values());
    }

    /** 创建模型
     * @param model 模型信息 */
    public void createModel(ModelEntity model) {
        modelRepository.insert(model);

        // 发布模型创建事件
        eventPublisher.publishEvent(new ModelCreatedEvent(model.getId(), model.getUserId(), model));
    }

    /** 修改模型
     * @param model 模型信息 */
    public void updateModel(ModelEntity model) {
        Wrapper<ModelEntity> wrapper = Wrappers.<ModelEntity>lambdaQuery().eq(ModelEntity::getId, model.getId())
                .eq(ModelEntity::getUserId, model.getUserId());
        modelRepository.checkedUpdate(model, wrapper);

        // 发布模型更新事件
        eventPublisher.publishEvent(new ModelUpdatedEvent(model.getId(), model.getUserId(), model));
    }

    /** 删除模型
     * @param modelId 模型id */
    public void deleteModel(String modelId, String userId, Operator operator) {
        Wrapper<ModelEntity> wrapper = Wrappers.<ModelEntity>lambdaQuery().eq(ModelEntity::getId, modelId)
                .eq(operator.needCheckUserId(), ModelEntity::getUserId, userId);
        modelRepository.checkedDelete(wrapper);

        // 发布模型删除事件
        eventPublisher.publishEvent(new ModelDeletedEvent(modelId, userId));
    }

    /** 修改模型状态
     * @param modelId 模型id
     * @param userId 用户id */
    public void updateModelStatus(String modelId, String userId) {
        // 先获取当前模型信息，用于判断状态变更
        ModelEntity currentModel = getModelById(modelId);
        boolean currentStatus = currentModel.getStatus();
        boolean newStatus = !currentStatus; // 状态取反

        LambdaUpdateWrapper<ModelEntity> updateWrapper = Wrappers.lambdaUpdate(ModelEntity.class)
                .eq(ModelEntity::getId, modelId).eq(ModelEntity::getUserId, userId).setSql("status = NOT status");

        modelRepository.checkedUpdate(updateWrapper);

        // 获取更新后的模型信息
        ModelEntity updatedModel = getModelById(modelId);

        // 发布模型状态变更事件
        eventPublisher.publishEvent(new ModelStatusChangedEvent(modelId, userId, updatedModel, newStatus, ""));
    }

    /** 根据类型获取服务商
     * @param providerType 服务商类型编码
     * @param userId 用户ID
     * @return 服务商聚合根列表 */
    public List<ProviderAggregate> getProvidersByType(ProviderType providerType, String userId) {
        LambdaQueryWrapper<ProviderEntity> wrapper = Wrappers.lambdaQuery();

        switch (providerType) {
            case OFFICIAL :
                wrapper.eq(ProviderEntity::getIsOfficial, true);
                break;
            case CUSTOM :
                wrapper.eq(ProviderEntity::getUserId, userId).eq(ProviderEntity::getIsOfficial, false);
                break;
            case ALL :
            default :
                wrapper.eq(ProviderEntity::getUserId, userId).or().eq(ProviderEntity::getIsOfficial, true);
        }

        return buildProviderAggregatesWithActiveModels(providerRepository.selectList(wrapper));
    }

    /** 修改服务商状态
     * @param providerId 服务商id
     * @param userId 用户id */
    public void updateProviderStatus(String providerId, String userId) {
        LambdaUpdateWrapper<ProviderEntity> updateWrapper = Wrappers.lambdaUpdate(ProviderEntity.class)
                .eq(ProviderEntity::getId, providerId).eq(ProviderEntity::getUserId, userId)
                .setSql("status = NOT status");
        providerRepository.checkedUpdate(updateWrapper);
    }

    /** 获取模型
     * @param modelId 模型id */
    public ModelEntity getModelById(String modelId) {
        ModelEntity modelEntity = modelRepository.selectById(modelId);
        if (modelEntity == null) {
            throw new BusinessException("模型不存在");
        }
        return modelEntity;
    }

    public ModelEntity findModelById(String modelId) {
        ModelEntity modelEntity = modelRepository.selectById(modelId);
        if (modelEntity == null) {
           return new ModelEntity();
        }
        return modelEntity;
    }



    /** 获取所有激活的模型
     * @return 所有激活的模型列表 */
    public List<ModelEntity> getAllActiveModels() {
        Wrapper<ModelEntity> wrapper = Wrappers.<ModelEntity>lambdaQuery().eq(ModelEntity::getStatus, true);
        return modelRepository.selectList(wrapper);
    }
}