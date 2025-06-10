package org.xhy.infrastructure.highavailability.example;

import org.springframework.stereotype.Component;
import org.xhy.domain.llm.model.HighAvailabilityResult;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.service.HighAvailabilityDomainService;
import org.xhy.domain.llm.service.LLMDomainService;

/** 会话亲和性使用示例
 * 
 * @author xhy
 * @since 1.0.0 */
@Component
public class SessionAffinityExample {

    private final HighAvailabilityDomainService highAvailabilityDomainService;
    private final LLMDomainService llmDomainService;

    public SessionAffinityExample(HighAvailabilityDomainService highAvailabilityDomainService,
            LLMDomainService llmDomainService) {
        this.highAvailabilityDomainService = highAvailabilityDomainService;
        this.llmDomainService = llmDomainService;
    }

    /** 演示会话亲和性的使用
     * 
     * @param modelId 模型ID
     * @param userId 用户ID
     * @param sessionId 会话ID */
    public void demonstrateSessionAffinity(String modelId, String userId, String sessionId) {
        // 获取模型
        ModelEntity model = llmDomainService.getModelById(modelId);

        // 第一次调用 - 会选择一个实例并建立亲和性
        HighAvailabilityResult firstResult = highAvailabilityDomainService.selectBestProvider(model, userId, sessionId);
        System.out.println("第一次调用选择的实例ID: " + firstResult.getInstanceId());

        // 第二次调用 - 应该返回相同的实例（如果实例健康）
        HighAvailabilityResult secondResult = highAvailabilityDomainService.selectBestProvider(model, userId, sessionId);
        System.out.println("第二次调用选择的实例ID: " + secondResult.getInstanceId());

        // 验证是否使用了相同的实例
        if (firstResult.getInstanceId() != null && firstResult.getInstanceId().equals(secondResult.getInstanceId())) {
            System.out.println("✅ 会话亲和性生效：两次调用使用了相同的实例");
        } else {
            System.out.println("⚠️ 会话亲和性未生效：两次调用使用了不同的实例");
        }

        // 不同会话ID的调用 - 可能会选择不同的实例
        String differentSessionId = sessionId + "_different";
        HighAvailabilityResult differentSessionResult = highAvailabilityDomainService.selectBestProvider(model, userId, differentSessionId);
        System.out.println("不同会话调用选择的实例ID: " + differentSessionResult.getInstanceId());
    }

    /** 演示不使用会话亲和性的情况
     * 
     * @param modelId 模型ID
     * @param userId 用户ID */
    public void demonstrateWithoutAffinity(String modelId, String userId) {
        // 获取模型
        ModelEntity model = llmDomainService.getModelById(modelId);

        // 不传递sessionId，使用普通负载均衡
        HighAvailabilityResult result1 = highAvailabilityDomainService.selectBestProvider(model, userId);
        HighAvailabilityResult result2 = highAvailabilityDomainService.selectBestProvider(model, userId);

        System.out.println("无亲和性第一次调用实例ID: " + result1.getInstanceId());
        System.out.println("无亲和性第二次调用实例ID: " + result2.getInstanceId());
    }
} 