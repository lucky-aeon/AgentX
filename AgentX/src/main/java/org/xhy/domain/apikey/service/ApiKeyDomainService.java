package org.xhy.domain.apikey.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.domain.apikey.model.ApiKeyEntity;
import org.xhy.domain.apikey.repository.ApiKeyRepository;
import org.xhy.infrastructure.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.List;

/** API密钥领域服务 */
@Service
public class ApiKeyDomainService {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyDomainService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    /** 创建API密钥
     *
     * @param apiKeyEntity API密钥实体
     * @return 创建后的API密钥实体 */
    @Transactional
    public ApiKeyEntity createApiKey(ApiKeyEntity apiKeyEntity) {
        // 生成API Key
        apiKeyEntity.generateApiKey();

        // 保存API密钥
        apiKeyRepository.checkInsert(apiKeyEntity);

        return apiKeyEntity;
    }

    /** 根据API Key查找
     *
     * @param apiKey API密钥
     * @return API密钥实体 */
    public ApiKeyEntity findByApiKey(String apiKey) {
        Wrapper<ApiKeyEntity> wrapper = Wrappers.<ApiKeyEntity>lambdaQuery().eq(ApiKeyEntity::getApiKey, apiKey);
        return apiKeyRepository.selectOne(wrapper);
    }

    /** 验证API Key
     *
     * @param apiKey API密钥
     * @return API密钥实体，如果无效则抛出异常 */
    public ApiKeyEntity validateApiKey(String apiKey) {
        ApiKeyEntity apiKeyEntity = findByApiKey(apiKey);

        if (apiKeyEntity == null) {
            throw new BusinessException("无效的API Key");
        }

        if (!apiKeyEntity.isAvailable()) {
            throw new BusinessException("API Key已禁用或过期");
        }

        return apiKeyEntity;
    }

    /** 更新API Key使用记录
     *
     * @param apiKey API密钥 */
    @Transactional
    public void updateUsage(String apiKey) {
        LambdaUpdateWrapper<ApiKeyEntity> wrapper = Wrappers.<ApiKeyEntity>lambdaUpdate()
                .eq(ApiKeyEntity::getApiKey, apiKey).setSql("usage_count = usage_count + 1")
                .set(ApiKeyEntity::getLastUsedAt, LocalDateTime.now());

        apiKeyRepository.checkedUpdate(wrapper);
    }

    /** 获取用户的API密钥列表
     *
     * @param userId 用户ID
     * @return API密钥列表 */
    public List<ApiKeyEntity> getUserApiKeys(String userId) {
        LambdaQueryWrapper<ApiKeyEntity> wrapper = Wrappers.<ApiKeyEntity>lambdaQuery()
                .eq(ApiKeyEntity::getUserId, userId).orderByDesc(ApiKeyEntity::getCreatedAt);
        return apiKeyRepository.selectList(wrapper);
    }

    /** 获取Agent的API密钥列表
     *
     * @param agentId Agent ID
     * @param userId 用户ID
     * @return API密钥列表 */
    public List<ApiKeyEntity> getAgentApiKeys(String agentId, String userId) {
        LambdaQueryWrapper<ApiKeyEntity> wrapper = Wrappers.<ApiKeyEntity>lambdaQuery()
                .eq(ApiKeyEntity::getAgentId, agentId).eq(ApiKeyEntity::getUserId, userId)
                .orderByDesc(ApiKeyEntity::getCreatedAt);
        return apiKeyRepository.selectList(wrapper);
    }

    /** 获取API密钥详情
     *
     * @param apiKeyId API密钥ID
     * @param userId 用户ID
     * @return API密钥实体 */
    public ApiKeyEntity getApiKey(String apiKeyId, String userId) {
        Wrapper<ApiKeyEntity> wrapper = Wrappers.<ApiKeyEntity>lambdaQuery().eq(ApiKeyEntity::getId, apiKeyId)
                .eq(ApiKeyEntity::getUserId, userId);

        ApiKeyEntity apiKeyEntity = apiKeyRepository.selectOne(wrapper);
        if (apiKeyEntity == null) {
            throw new BusinessException("API密钥不存在: " + apiKeyId);
        }

        return apiKeyEntity;
    }

    /** 更新API密钥状态
     *
     * @param apiKeyId API密钥ID
     * @param userId 用户ID
     * @param status 状态 */
    @Transactional
    public void updateStatus(String apiKeyId, String userId, Boolean status) {
        LambdaUpdateWrapper<ApiKeyEntity> wrapper = Wrappers.<ApiKeyEntity>lambdaUpdate()
                .eq(ApiKeyEntity::getId, apiKeyId).eq(ApiKeyEntity::getUserId, userId)
                .set(ApiKeyEntity::getStatus, status);

        apiKeyRepository.checkedUpdate(wrapper);
    }

    /** 删除API密钥
     *
     * @param apiKeyId API密钥ID
     * @param userId 用户ID */
    @Transactional
    public void deleteApiKey(String apiKeyId, String userId) {
        Wrapper<ApiKeyEntity> wrapper = Wrappers.<ApiKeyEntity>lambdaQuery().eq(ApiKeyEntity::getId, apiKeyId)
                .eq(ApiKeyEntity::getUserId, userId);

        apiKeyRepository.checkedDelete(wrapper);
    }

    /** 重置API密钥
     *
     * @param apiKeyId API密钥ID
     * @param userId 用户ID
     * @return 新的API密钥实体 */
    @Transactional
    public ApiKeyEntity resetApiKey(String apiKeyId, String userId) {
        ApiKeyEntity apiKeyEntity = getApiKey(apiKeyId, userId);

        // 生成新的API Key
        apiKeyEntity.generateApiKey();
        apiKeyEntity.setUsageCount(0);
        apiKeyEntity.setLastUsedAt(null);

        LambdaUpdateWrapper<ApiKeyEntity> wrapper = Wrappers.<ApiKeyEntity>lambdaUpdate()
                .eq(ApiKeyEntity::getId, apiKeyId).eq(ApiKeyEntity::getUserId, userId);

        apiKeyRepository.update(apiKeyEntity, wrapper);

        return apiKeyEntity;
    }
}