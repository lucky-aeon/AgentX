package org.xhy.domain.llm.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.domain.llm.model.config.ProviderConfig;
import org.xhy.domain.llm.model.enums.ProviderCode;
import org.xhy.domain.llm.repository.ModelRepository;
import org.xhy.domain.llm.repository.ProviderRepository;
import org.xhy.infrastructure.util.JsonUtils;
import org.xhy.infrastructure.utils.EncryptionUtil;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * LLM领域服务
 * 负责服务提供商和模型的核心业务逻辑
 */
@Service
public class LlmDomainService {
    
    private final ProviderRepository providerRepository;
    private final ModelRepository modelRepository;
    private final EncryptionUtil encryptionUtil;

    public LlmDomainService(
            ProviderRepository providerRepository,
            ModelRepository modelRepository,
            EncryptionUtil encryptionUtil) {
        this.providerRepository = providerRepository;
        this.modelRepository = modelRepository;
        this.encryptionUtil = encryptionUtil;
    }
    
    /**
     * 创建服务商
     * @param provider 服务商信息
     * @return 创建后的服务商ID
     */
    public String createProvider(ProviderEntity provider) {
        // 1. 验证服务商协议是否支持
        validateProviderCode(provider.getCode());
        
        // 2. 加密配置信息
        encryptProviderConfig(provider);
        
        // 3. 保存服务商信息
        providerRepository.insert(provider);
        
        return provider.getId();
    }
    
    
    /**
     * 更新服务商
     * @param provider 服务商信息
     */
    public void updateProvider(ProviderEntity provider) {
        // 1. 验证服务商协议是否支持
        validateProviderCode(provider.getCode());
        
        // 2. 加密配置信息
        encryptProviderConfig(provider);

        // 3. 更新服务商信息
        providerRepository.updateById(provider);
    }

    /**
     * 获取服务商
     * @param providerId 服务商id
     * @param userId 用户id
     * @return
     */
    public ProviderEntity getProvider(String providerId, String userId) {

        Wrapper<ProviderEntity> wrapper = Wrappers.<ProviderEntity>lambdaQuery().eq(ProviderEntity::getId, providerId).eq(ProviderEntity::getUserId, userId);
        ProviderEntity provider = providerRepository.selectOne(wrapper);
        if (provider == null) {
            throw new IllegalArgumentException("服务商不存在");
        }
        // 解密
        ProviderConfig config = provider.getConfig();
        if (config != null) {
            String decryptedConfig = encryptionUtil.decrypt(config.getApiKey());
            config.setApiKey(decryptedConfig);
            provider.setConfig(config);
        }
        return provider;
    }

    /**
     * 删除服务商
     * @param providerId 服务商id
     * @param userId 用户id
     */
    @Transactional
    public void deleteProvider(String providerId, String userId){
        Wrapper<ProviderEntity> wrapper = Wrappers.<ProviderEntity>lambdaQuery().eq(ProviderEntity::getId, providerId).eq(ProviderEntity::getUserId, userId);
        providerRepository.delete(wrapper);
        // 删除模型
        Wrapper<ModelEntity> modelWrapper = Wrappers.<ModelEntity>lambdaQuery().eq(ModelEntity::getProviderId, providerId);
        modelRepository.delete(modelWrapper);
    }

    /**
     * 验证服务商协议是否支持
     */
    private void validateProviderCode(String code) {
        // TODO: 从配置或枚举中获取支持的服务商协议列表
        if (!isSupportedProvider(code)) {
            throw new IllegalArgumentException("不支持的服务商协议类型: " + code);
        }
    }
    
    /**
     * 检查是否是支持的服务商协议
     */
    private boolean isSupportedProvider(String code) {
        // TODO: 实现服务商协议的检查逻辑
        // 这里可以通过配置文件或者枚举来维护支持的协议列表
        // 修复了ProviderCode的引用错误
        ProviderCode[] supportedProviderTypes = ProviderCode.values();
        return Arrays.stream(supportedProviderTypes)
                .anyMatch(providerType -> providerType.name().equals(code));
    }
    
    /**
     * 加密服务商配置
     */
    private void encryptProviderConfig(ProviderEntity provider) {
        ProviderConfig config = provider.getConfig();
        if (config != null) {
            // 将配置对象转换为JSON字符串

            String configJson = JsonUtils.toJsonString(config);
            // 加密配置信息
            String encryptedConfig = encryptionUtil.encrypt(configJson);
            // 将加密后的配置信息存回对象
            config.setApiKey(encryptedConfig);
            provider.setConfig(config);
        }
    }
} 