package org.xhy.infrastructure.billing.strategy;

import java.math.BigDecimal;
import java.util.Map;

/** 规则策略接口 */
public interface RuleStrategy {

    /** 计算费用
     * 
     * @param usageData 用量数据 (如：{"input": 1000, "output": 500}、{"quantity": 1})
     * @param pricingConfig 价格配置 (如：{"input_cost_per_million": 5.0, "output_cost_per_million": 15.0})
     * @return 计算出的费用 */
    BigDecimal process(Map<String, Object> usageData, Map<String, Object> pricingConfig);

    /** 获取策略名称
     * 
     * @return 策略名称 */
    String getStrategyName();

    /** 验证用量数据是否有效
     * 
     * @param usageData 用量数据
     * @return 是否有效 */
    boolean validateUsageData(Map<String, Object> usageData);

    /** 验证价格配置是否有效
     * 
     * @param pricingConfig 价格配置
     * @return 是否有效 */
    boolean validatePricingConfig(Map<String, Object> pricingConfig);
}