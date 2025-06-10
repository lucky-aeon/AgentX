package org.xhy.infrastructure.highavailability.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.xhy.domain.llm.event.ModelStatusChangedEvent;
import org.xhy.domain.llm.model.HighAvailabilityResult;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.domain.llm.service.HighAvailabilityDomainService;
import org.xhy.domain.llm.service.LLMDomainService;
import org.xhy.infrastructure.config.HighAvailabilityProperties;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.highavailability.client.HighAvailabilityGatewayClient;
import org.xhy.infrastructure.highavailability.dto.request.ApiInstanceCreateRequest;
import org.xhy.infrastructure.highavailability.dto.request.ApiInstanceUpdateRequest;
import org.xhy.infrastructure.highavailability.dto.request.ProjectCreateRequest;
import org.xhy.infrastructure.highavailability.dto.request.ReportResultRequest;
import org.xhy.infrastructure.highavailability.dto.request.SelectInstanceRequest;
import org.xhy.infrastructure.highavailability.dto.response.ApiInstanceDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * 高可用领域服务实现
 * 
 * @author xhy
 * @since 1.0.0
 */
@Service
public class HighAvailabilityDomainServiceImpl implements HighAvailabilityDomainService {

    private static final Logger logger = LoggerFactory.getLogger(HighAvailabilityDomainServiceImpl.class);

    private final HighAvailabilityProperties properties;
    private final HighAvailabilityGatewayClient gatewayClient;
    private final LLMDomainService llmDomainService;
    private final ApplicationEventPublisher eventPublisher;

    public HighAvailabilityDomainServiceImpl(HighAvailabilityProperties properties,
                                             HighAvailabilityGatewayClient gatewayClient,
                                             LLMDomainService llmDomainService,
                                             ApplicationEventPublisher eventPublisher) {
        this.properties = properties;
        this.gatewayClient = gatewayClient;
        this.llmDomainService = llmDomainService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void syncModelToGateway(ModelEntity model) {
        if (!properties.isEnabled()) {
            logger.debug("高可用功能未启用，跳过模型同步: {}", model.getId());
            return;
        }

        try {
            ApiInstanceCreateRequest request = new ApiInstanceCreateRequest(
                model.getUserId(),
                model.getModelId(),
                "MODEL",
                model.getId()
            );

            gatewayClient.createApiInstance(request);
            
            logger.info("成功同步模型到高可用网关: modelId={}", model.getId());

        } catch (Exception e) {
            logger.error("同步模型到高可用网关失败: modelId={}", model.getId(), e);
            throw new BusinessException("同步模型到高可用网关失败", e);
        }
    }

    @Override
    public void removeModelFromGateway(String modelId, String userId) {
        if (!properties.isEnabled()) {
            logger.debug("高可用功能未启用，跳过模型删除: {}", modelId);
            return;
        }

        try {
            gatewayClient.deleteApiInstance("MODEL", modelId);
            
            logger.info("成功从高可用网关删除模型: modelId={}", modelId);

        } catch (Exception e) {
            logger.error("从高可用网关删除模型失败: modelId={}", modelId, e);
            // 删除失败不抛异常，避免影响主流程
        }
    }

    @Override
    public void updateModelInGateway(ModelEntity model) {
        if (!properties.isEnabled()) {
            logger.debug("高可用功能未启用，跳过模型更新: {}", model.getId());
            return;
        }

        try {
            ApiInstanceUpdateRequest request = new ApiInstanceUpdateRequest(
                model.getUserId(),
                model.getModelId(),
                null, // routingParams
                null  // metadata
            );

            gatewayClient.updateApiInstance("MODEL", model.getId(), request);
            
            logger.info("成功更新高可用网关中的模型: modelId={}", model.getId());

        } catch (Exception e) {
            logger.error("更新高可用网关中的模型失败: modelId={}", model.getId(), e);
            // 更新失败不抛异常，避免影响主流程
        }
    }

    @Override
    public HighAvailabilityResult selectBestProvider(ModelEntity model, String userId) {
        if (!properties.isEnabled()) {
            // 高可用未启用，使用默认逻辑
            logger.debug("高可用功能未启用，使用默认Provider选择逻辑: modelId={}", model.getId());
            ProviderEntity provider = llmDomainService.getProvider(model.getProviderId(), userId);
            return new HighAvailabilityResult(provider, model, null);
        }

        try {
            // 构建选择实例请求
            SelectInstanceRequest request = new SelectInstanceRequest(
                userId,
                model.getModelId(),
                "MODEL"
            );

            // 通过高可用网关选择最佳实例，客户端已经处理了响应解析
            ApiInstanceDTO selectedInstance = gatewayClient.selectBestInstance(request);
            
            String businessId = selectedInstance.getBusinessId();
            String instanceId = selectedInstance.getId();

            // 获取最佳实例对应的模型
            ModelEntity bestModel = llmDomainService.getModelById(businessId);
            
            // 返回最佳模型对应的Provider
            ProviderEntity provider = llmDomainService.getProvider(bestModel.getProviderId(), userId);
            
            logger.info("通过高可用网关选择Provider成功: modelId={}, bestBusinessId={}, providerId={}", 
                model.getId(), businessId, provider.getId());
            
            return new HighAvailabilityResult(provider, bestModel, instanceId);

        } catch (Exception e) {
            logger.warn("高可用网关选择Provider失败，降级到默认逻辑: modelId={}", model.getId(), e);
            
            // 降级处理：使用默认逻辑
            try {
                ProviderEntity provider = llmDomainService.getProvider(model.getProviderId(), userId);
                return new HighAvailabilityResult(provider, model, null);
            } catch (Exception fallbackException) {
                logger.error("降级逻辑也失败了: modelId={}", model.getId(), fallbackException);
                throw new BusinessException("获取Provider失败", fallbackException);
            }
        }
    }

    @Override
    @Async
    public void reportCallResult(String instanceId, String modelId, boolean success, long latencyMs, String errorMessage) {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            ReportResultRequest request = new ReportResultRequest();
            request.setInstanceId(instanceId); // 使用真正的实例ID
            request.setBusinessId(modelId);
            request.setSuccess(success);
            request.setLatencyMs(latencyMs);
            request.setErrorMessage(errorMessage);
            request.setCallTimestamp(System.currentTimeMillis());

            gatewayClient.reportResult(request);
            
            logger.debug("成功上报调用结果: instanceId={}, modelId={}, success={}, latency={}ms", 
                instanceId, modelId, success, latencyMs);

        } catch (Exception e) {
            logger.error("上报调用结果失败: instanceId={}, modelId={}", instanceId, modelId, e);
            // 上报失败不抛异常
        }
    }

    @Override
    public void initializeProject() {
        if (!properties.isEnabled()) {
            logger.info("高可用功能未启用，跳过项目初始化");
            return;
        }

        try {
            // 创建项目
            ProjectCreateRequest projectRequest = new ProjectCreateRequest(
                "AgentX",
                "AgentX高可用项目",
                properties.getApiKey()
            );

            gatewayClient.createProject(projectRequest);
            
            logger.info("高可用项目初始化成功");

        } catch (Exception e) {
            logger.error("高可用项目初始化失败", e);
            // 初始化失败不抛异常，允许系统继续运行
        }
    }

    @Override
    public void syncAllModelsToGateway() {
        if (!properties.isEnabled()) {
            logger.info("高可用功能未启用，跳过模型批量同步");
            return;
        }

        try {
            // 获取所有激活的模型
            List<ModelEntity> allActiveModels = llmDomainService.getAllActiveModels();
            
            if (allActiveModels.isEmpty()) {
                logger.info("没有激活的模型需要同步");
                return;
            }

            // 构建批量创建请求列表
            List<ApiInstanceCreateRequest> instanceRequests = new ArrayList<>();
            for (ModelEntity model : allActiveModels) {
                ApiInstanceCreateRequest request = new ApiInstanceCreateRequest(
                    model.getUserId(),
                    model.getModelId(),
                    "MODEL",
                    model.getId()
                );
                instanceRequests.add(request);
            }

            // 批量同步到高可用网关
            gatewayClient.batchCreateApiInstances(instanceRequests);
            
            logger.info("成功批量同步{}个模型到高可用网关", allActiveModels.size());

        } catch (Exception e) {
            logger.error("批量同步模型到高可用网关失败", e);
            // 批量同步失败不抛异常，允许系统继续运行
        }
    }

    @Override
    public void changeModelStatusInGateway(ModelEntity model, boolean enabled, String reason) {
        if (!properties.isEnabled()) {
            logger.debug("高可用功能未启用，跳过模型状态变更: {}", model.getId());
            return;
        }

        try {
            if (enabled) {
                // 启用模型
                gatewayClient.activateApiInstance("MODEL", model.getId());
                logger.info("成功启用高可用网关中的模型: modelId={}, reason={}", model.getId(), reason);
            } else {
                // 禁用模型
                gatewayClient.deactivateApiInstance("MODEL", model.getId());
                logger.info("成功禁用高可用网关中的模型: modelId={}, reason={}", model.getId(), reason);
            }

            // 注意：这里不再发布事件，避免循环调用
            // 事件发布应该在LLMDomainService.updateModelStatus()中进行

        } catch (Exception e) {
            logger.error("变更高可用网关中的模型状态失败: modelId={}, enabled={}", 
                model.getId(), enabled, e);
            // 状态变更失败不抛异常，避免影响主流程
        }
    }
} 