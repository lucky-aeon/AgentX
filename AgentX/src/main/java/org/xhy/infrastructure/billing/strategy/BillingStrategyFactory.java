package org.xhy.infrastructure.billing.strategy;

import org.springframework.stereotype.Component;
import org.xhy.domain.rule.constant.RuleHandlerKey;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 计费策略工厂 管理所有计费策略实例，根据handler_key返回对应策略 */
@Component
public class BillingStrategyFactory {

    private final Map<String, RuleStrategy> strategyMap;

    /** 构造函数，自动注册所有策略实例 */
    public BillingStrategyFactory(List<RuleStrategy> strategies) {
        this.strategyMap = new HashMap<>();

        // 注册所有策略实例
        for (RuleStrategy strategy : strategies) {
            strategyMap.put(strategy.getStrategyName(), strategy);
        }
    }

    /** 根据handler_key获取对应的策略实例
     * 
     * @param handlerKey 规则处理器标识
     * @return 计费策略实例
     * @throws BusinessException 如果策略不存在 */
    public RuleStrategy getStrategy(String handlerKey) {
        // 验证handler_key是否有效
        if (!RuleHandlerKey.isValidKey(handlerKey)) {
            throw new BusinessException("无效的规则处理器标识: " + handlerKey);
        }

        RuleStrategy strategy = strategyMap.get(handlerKey);
        if (strategy == null) {
            throw new BusinessException("未找到对应的计费策略: " + handlerKey);
        }

        return strategy;
    }

    /** 根据RuleHandlerKey枚举获取策略实例
     * 
     * @param handlerKey 规则处理器枚举
     * @return 计费策略实例 */
    public RuleStrategy getStrategy(RuleHandlerKey handlerKey) {
        return getStrategy(handlerKey.getKey());
    }

    /** 检查策略是否存在
     * 
     * @param handlerKey 规则处理器标识
     * @return 是否存在 */
    public boolean hasStrategy(String handlerKey) {
        return RuleHandlerKey.isValidKey(handlerKey) && strategyMap.containsKey(handlerKey);
    }

    /** 获取所有已注册的策略名称
     * 
     * @return 策略名称集合 */
    public java.util.Set<String> getAllStrategyNames() {
        return strategyMap.keySet();
    }
}