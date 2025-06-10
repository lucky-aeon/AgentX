package org.xhy.application.llm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.xhy.domain.llm.event.ModelCreatedEvent;
import org.xhy.domain.llm.event.ModelDeletedEvent;
import org.xhy.domain.llm.event.ModelStatusChangedEvent;
import org.xhy.domain.llm.event.ModelUpdatedEvent;
import org.xhy.domain.llm.service.HighAvailabilityDomainService;

/**
 * 高可用事件处理器
 * 负责处理模型相关的领域事件，同步到高可用网关
 * 
 * @author xhy
 * @since 1.0.0
 */
@Component
public class HighAvailabilityEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(HighAvailabilityEventHandler.class);

    private final HighAvailabilityDomainService highAvailabilityDomainService;

    public HighAvailabilityEventHandler(HighAvailabilityDomainService highAvailabilityDomainService) {
        this.highAvailabilityDomainService = highAvailabilityDomainService;
    }

    /**
     * 处理模型创建事件
     * 将新创建的模型同步到高可用网关
     */
    @EventListener
    @Async
    public void handleModelCreated(ModelCreatedEvent event) {
        try {
            logger.info("处理模型创建事件: modelId={}, userId={}", event.getModelId(), event.getUserId());
            
            highAvailabilityDomainService.syncModelToGateway(event.getModel());
            
            logger.info("模型创建事件处理成功: modelId={}", event.getModelId());
            
        } catch (Exception e) {
            logger.error("处理模型创建事件失败: modelId={}", event.getModelId(), e);
        }
    }

    /**
     * 处理模型更新事件
     * 将更新的模型信息同步到高可用网关
     */
    @EventListener
    @Async
    public void handleModelUpdated(ModelUpdatedEvent event) {
        try {
            logger.info("处理模型更新事件: modelId={}, userId={}", event.getModelId(), event.getUserId());
            
            highAvailabilityDomainService.updateModelInGateway(event.getModel());
            
            logger.info("模型更新事件处理成功: modelId={}", event.getModelId());
            
        } catch (Exception e) {
            logger.error("处理模型更新事件失败: modelId={}", event.getModelId(), e);
        }
    }

    /**
     * 处理模型删除事件
     * 从高可用网关中删除模型
     */
    @EventListener
    @Async
    public void handleModelDeleted(ModelDeletedEvent event) {
        try {
            logger.info("处理模型删除事件: modelId={}, userId={}", event.getModelId(), event.getUserId());
            
            highAvailabilityDomainService.removeModelFromGateway(event.getModelId(), event.getUserId());
            
            logger.info("模型删除事件处理成功: modelId={}", event.getModelId());
            
        } catch (Exception e) {
            logger.error("处理模型删除事件失败: modelId={}", event.getModelId(), e);
        }
    }

    /**
     * 处理模型状态变更事件
     * 将模型状态变更同步到高可用网关（启用/禁用）
     */
    @EventListener
    @Async
    public void handleModelStatusChanged(ModelStatusChangedEvent event) {
        try {
            logger.info("处理模型状态变更事件: modelId={}, userId={}, enabled={}, reason={}", 
                event.getModelId(), event.getUserId(), event.isEnabled(), event.getReason());
            
            // 直接调用高可用领域服务来同步状态变更到网关
            // 注意：这里不会引起循环，因为changeModelStatusInGateway不再发布事件
            highAvailabilityDomainService.changeModelStatusInGateway(
                event.getModel(), 
                event.isEnabled(), 
                event.getReason()
            );
            
            logger.info("模型状态变更事件处理成功: modelId={}, enabled={}", 
                event.getModelId(), event.isEnabled());
            
        } catch (Exception e) {
            logger.error("处理模型状态变更事件失败: modelId={}, enabled={}", 
                event.getModelId(), event.isEnabled(), e);
        }
    }
} 