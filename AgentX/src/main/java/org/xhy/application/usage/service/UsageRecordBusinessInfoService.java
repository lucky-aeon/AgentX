package org.xhy.application.usage.service;

import org.springframework.stereotype.Service;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.agent.service.AgentDomainService;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.service.LLMDomainService;
import org.xhy.domain.product.constant.BillingType;
import org.xhy.domain.product.model.ProductEntity;
import org.xhy.domain.product.service.ProductDomainService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 用量记录业务信息服务 负责高效映射业务信息，避免循环查询 */
@Service
public class UsageRecordBusinessInfoService {

    private final ProductDomainService productDomainService;
    private final LLMDomainService llmDomainService;
    private final AgentDomainService agentDomainService;

    public UsageRecordBusinessInfoService(ProductDomainService productDomainService, LLMDomainService llmDomainService,
            AgentDomainService agentDomainService) {
        this.productDomainService = productDomainService;
        this.llmDomainService = llmDomainService;
        this.agentDomainService = agentDomainService;
    }

    /** 业务信息数据结构 */
    public static class BusinessInfo {
        private String serviceName;
        private String serviceType;
        private String serviceDescription;
        private String pricingRule;
        private String relatedEntityName;

        public BusinessInfo(String serviceName, String serviceType, String serviceDescription, String pricingRule,
                String relatedEntityName) {
            this.serviceName = serviceName;
            this.serviceType = serviceType;
            this.serviceDescription = serviceDescription;
            this.pricingRule = pricingRule;
            this.relatedEntityName = relatedEntityName;
        }

        // Getters
        public String getServiceName() {
            return serviceName;
        }
        public String getServiceType() {
            return serviceType;
        }
        public String getServiceDescription() {
            return serviceDescription;
        }
        public String getPricingRule() {
            return pricingRule;
        }
        public String getRelatedEntityName() {
            return relatedEntityName;
        }
    }

    /** 批量获取商品业务信息映射，避免循环查询
     * 
     * @param productIds 商品ID集合
     * @return productId -> BusinessInfo 的映射 */
    public Map<String, BusinessInfo> getBatchBusinessInfo(Set<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return new HashMap<>();
        }

        // 1. 批量查询商品信息
        Map<String, ProductEntity> productMap = productDomainService.getProductsByIds(productIds).stream()
                .collect(Collectors.toMap(ProductEntity::getId, product -> product));

        // 2. 按类型分组收集需要查询的实体ID
        Set<String> modelIds = productMap.values().stream()
                .filter(product -> product.getType() == BillingType.MODEL_USAGE).map(ProductEntity::getServiceId)
                .collect(Collectors.toSet());

        Set<String> agentIds = productMap.values().stream()
                .filter(product -> product.getType() == BillingType.AGENT_USAGE).map(ProductEntity::getServiceId)
                .collect(Collectors.toSet());

        // 3. 批量查询关联实体
        Map<String, ModelEntity> modelMap = new HashMap<>();
        if (!modelIds.isEmpty()) {
            modelMap = llmDomainService.getModelsByIds(modelIds).stream()
                    .collect(Collectors.toMap(ModelEntity::getId, model -> model));
        }

        Map<String, AgentEntity> agentMap = new HashMap<>();
        if (!agentIds.isEmpty()) {
            agentMap = agentDomainService.getAgentsByIds(agentIds).stream()
                    .collect(Collectors.toMap(AgentEntity::getId, agent -> agent));
        }

        // 4. 构建业务信息映射
        Map<String, BusinessInfo> businessInfoMap = new HashMap<>();
        for (String productId : productIds) {
            ProductEntity product = productMap.get(productId);
            if (product != null) {
                BusinessInfo businessInfo = generateBusinessInfo(product, modelMap, agentMap);
                businessInfoMap.put(productId, businessInfo);
            }
        }

        return businessInfoMap;
    }

    /** 根据商品和关联实体生成业务信息 */
    private BusinessInfo generateBusinessInfo(ProductEntity product, Map<String, ModelEntity> modelMap,
            Map<String, AgentEntity> agentMap) {
        BillingType billingType = product.getType();
        String serviceId = product.getServiceId();

        switch (billingType) {
            case MODEL_USAGE :
                return generateModelUsageInfo(product, modelMap.get(serviceId));
            case AGENT_CREATION :
                return generateAgentCreationInfo(product);
            case AGENT_USAGE :
                return generateAgentUsageInfo(product, agentMap.get(serviceId));
            case API_CALL :
                return generateApiCallInfo(product);
            case STORAGE_USAGE :
                return generateStorageUsageInfo(product);
            default :
                return generateDefaultInfo(product);
        }
    }

    /** 生成模型调用业务信息 */
    private BusinessInfo generateModelUsageInfo(ProductEntity product, ModelEntity model) {
        String modelName = model != null ? model.getName() : "未知模型";
        String serviceName = modelName + " 模型调用";
        String serviceType = "模型服务";
        String serviceDescription = "高级语言模型API调用服务";
        String pricingRule = formatModelPricingRule(product.getPricingConfig());
        String relatedEntityName = modelName;

        return new BusinessInfo(serviceName, serviceType, serviceDescription, pricingRule, relatedEntityName);
    }

    /** 生成Agent创建业务信息 */
    private BusinessInfo generateAgentCreationInfo(ProductEntity product) {
        String serviceName = "Agent 创建服务";
        String serviceType = "Agent服务";
        String serviceDescription = "智能助手创建服务";
        String pricingRule = formatPerUnitPricingRule(product.getPricingConfig(), "创建Agent数量");
        String relatedEntityName = "Agent创建";

        return new BusinessInfo(serviceName, serviceType, serviceDescription, pricingRule, relatedEntityName);
    }

    /** 生成Agent使用业务信息 */
    private BusinessInfo generateAgentUsageInfo(ProductEntity product, AgentEntity agent) {
        String agentName = agent != null ? agent.getName() : "未知Agent";
        String serviceName = agentName + " (Agent使用)";
        String serviceType = "Agent服务";
        String serviceDescription = "智能助手调用服务";
        String pricingRule = formatPerUnitPricingRule(product.getPricingConfig(), "调用次数");
        String relatedEntityName = agentName;

        return new BusinessInfo(serviceName, serviceType, serviceDescription, pricingRule, relatedEntityName);
    }

    /** 生成API调用业务信息 */
    private BusinessInfo generateApiCallInfo(ProductEntity product) {
        String serviceName = "API 调用服务";
        String serviceType = "API服务";
        String serviceDescription = "平台API接口调用服务";
        String pricingRule = formatPerUnitPricingRule(product.getPricingConfig(), "调用次数");
        String relatedEntityName = "API服务";

        return new BusinessInfo(serviceName, serviceType, serviceDescription, pricingRule, relatedEntityName);
    }

    /** 生成存储使用业务信息 */
    private BusinessInfo generateStorageUsageInfo(ProductEntity product) {
        String serviceName = "存储服务";
        String serviceType = "存储服务";
        String serviceDescription = "数据存储服务";
        String pricingRule = "按存储量计费";
        String relatedEntityName = "存储服务";

        return new BusinessInfo(serviceName, serviceType, serviceDescription, pricingRule, relatedEntityName);
    }

    /** 生成默认业务信息 */
    private BusinessInfo generateDefaultInfo(ProductEntity product) {
        String serviceName = "其他服务";
        String serviceType = "其他";
        String serviceDescription = "平台其他服务";
        String pricingRule = "详情请联系客服";
        String relatedEntityName = "其他";

        return new BusinessInfo(serviceName, serviceType, serviceDescription, pricingRule, relatedEntityName);
    }

    /** 格式化模型Token计费规则 - 重要：说明每百万token计费 */
    private String formatModelPricingRule(Map<String, Object> pricingConfig) {
        if (pricingConfig == null) {
            return "定价信息暂无";
        }

        Object inputCostObj = pricingConfig.get("input_cost_per_million");
        Object outputCostObj = pricingConfig.get("output_cost_per_million");

        if (inputCostObj != null && outputCostObj != null) {
            BigDecimal inputCostPerMillion = new BigDecimal(inputCostObj.toString());
            BigDecimal outputCostPerMillion = new BigDecimal(outputCostObj.toString());

            // 转换为每1K tokens的费用（除以1000）
            BigDecimal inputCostPer1K = inputCostPerMillion.divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
            BigDecimal outputCostPer1K = outputCostPerMillion.divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);

            return String.format("输入 ¥%.4f/1K tokens，输出 ¥%.4f/1K tokens（基于每百万token ¥%.2f/¥%.2f计算）", inputCostPer1K,
                    outputCostPer1K, inputCostPerMillion, outputCostPerMillion);
        }

        return "Token计费，详情请联系客服";
    }

    /** 格式化按次计费规则 */
    private String formatPerUnitPricingRule(Map<String, Object> pricingConfig, String unit) {
        if (pricingConfig == null) {
            return "定价信息暂无";
        }

        Object costPerUnitObj = pricingConfig.get("cost_per_unit");
        if (costPerUnitObj != null) {
            BigDecimal costPerUnit = new BigDecimal(costPerUnitObj.toString());
            return String.format("¥%.2f/%s", costPerUnit, unit);
        }

        return String.format("按%s计费，详情请联系客服", unit);
    }
}