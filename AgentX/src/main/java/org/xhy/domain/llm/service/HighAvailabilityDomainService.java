package org.xhy.domain.llm.service;

import org.xhy.domain.llm.model.HighAvailabilityResult;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.domain.llm.event.ModelsBatchDeletedEvent;

import java.util.List;

/** 高可用领域服务接口 定义高可用网关相关的领域操作
 * 
 * @author xhy
 * @since 1.0.0 */
public interface HighAvailabilityDomainService {

    /** 同步模型到高可用网关
     * 
     * @param model 模型实体 */
    void syncModelToGateway(ModelEntity model);

    /** 从高可用网关删除模型
     * 
     * @param modelId 模型ID
     * @param userId 用户ID */
    void removeModelFromGateway(String modelId, String userId);

    /** 更新高可用网关中的模型
     * 
     * @param model 模型实体 */
    void updateModelInGateway(ModelEntity model);

    /** 通过高可用网关选择最佳Provider和Model 如果高可用未启用或选择失败，则降级到默认逻辑
     * 
     * @param model 模型实体
     * @param userId 用户ID
     * @return 高可用选择结果（包含Provider和Model） */
    HighAvailabilityResult selectBestProvider(ModelEntity model, String userId);

    /** 通过高可用网关选择最佳Provider和Model（支持会话亲和性） 如果高可用未启用或选择失败，则降级到默认逻辑
     * 
     * @param model 模型实体
     * @param userId 用户ID
     * @param sessionId 会话ID，用于会话亲和性
     * @return 高可用选择结果（包含Provider和Model） */
    HighAvailabilityResult selectBestProvider(ModelEntity model, String userId, String sessionId);

    /** 通过高可用网关选择最佳Provider和Model（支持会话亲和性和降级链） 如果高可用未启用或选择失败，则降级到默认逻辑
     *
     * @param model 模型实体
     * @param userId 用户ID
     * @param sessionId 会话ID，用于会话亲和性
     * @param fallbackChain 降级模型链，为null时不启用降级
     * @return 高可用选择结果（包含Provider和Model） */
    HighAvailabilityResult selectBestProvider(ModelEntity model, String userId, String sessionId,
            List<String> fallbackChain);

    /** 上报调用结果到高可用网关
     * 
     * @param instanceId 实例ID（从selectBestProvider返回）
     * @param modelId 模型ID
     * @param success 是否成功
     * @param latencyMs 延迟时间(毫秒)
     * @param errorMessage 错误信息(可选) */
    void reportCallResult(String instanceId, String modelId, boolean success, long latencyMs, String errorMessage);

    /** 初始化项目到高可用网关 */
    void initializeProject();

    /** 批量同步所有模型到高可用网关 */
    void syncAllModelsToGateway();

    /** 变更模型在高可用网关中的状态
     * 
     * @param model 模型实体
     * @param enabled true=启用，false=禁用
     * @param reason 状态变更原因 */
    void changeModelStatusInGateway(ModelEntity model, boolean enabled, String reason);

    /** 批量从高可用网关删除模型
     * 
     * @param deleteItems 要删除的模型列表
     * @param userId 用户ID */
    void batchRemoveModelsFromGateway(List<ModelsBatchDeletedEvent.ModelDeleteItem> deleteItems, String userId);
}