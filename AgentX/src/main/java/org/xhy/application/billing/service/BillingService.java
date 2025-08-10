package org.xhy.application.billing.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.billing.dto.RuleContext;
import org.xhy.application.usage.service.UsageRecordBusinessInfoService;
import org.xhy.domain.product.model.ProductEntity;
import org.xhy.domain.product.service.ProductDomainService;
import org.xhy.domain.rule.model.RuleEntity;
import org.xhy.domain.rule.service.RuleDomainService;
import org.xhy.domain.user.model.UsageRecordEntity;
import org.xhy.domain.user.service.AccountDomainService;
import org.xhy.domain.user.service.UsageRecordDomainService;
import org.xhy.infrastructure.billing.strategy.RuleStrategy;
import org.xhy.infrastructure.billing.strategy.BillingStrategyFactory;
import org.xhy.infrastructure.exception.BusinessException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 计费服务 协调整个计费流程的核心服务 */
@Service
public class BillingService {

    private final ProductDomainService productDomainService;
    private final RuleDomainService ruleDomainService;
    private final AccountDomainService accountDomainService;
    private final UsageRecordDomainService usageRecordDomainService;
    private final BillingStrategyFactory billingStrategyFactory;
    private final UsageRecordBusinessInfoService businessInfoService;

    public BillingService(ProductDomainService productDomainService, RuleDomainService ruleDomainService,
            AccountDomainService accountDomainService, UsageRecordDomainService usageRecordDomainService,
            BillingStrategyFactory billingStrategyFactory, UsageRecordBusinessInfoService businessInfoService) {
        this.productDomainService = productDomainService;
        this.ruleDomainService = ruleDomainService;
        this.accountDomainService = accountDomainService;
        this.usageRecordDomainService = usageRecordDomainService;
        this.billingStrategyFactory = billingStrategyFactory;
        this.businessInfoService = businessInfoService;
    }

    /** 执行计费
     * 
     * @param context 计费上下文
     * @throws BusinessException 余额不足或其他业务异常 */
    @Transactional
    public void charge(RuleContext context) {
        // 1. 验证上下文
        if (!context.isValid()) {
            throw new BusinessException("无效的计费上下文");
        }

        // 2. 查找商品
        ProductEntity product = productDomainService.findProductByBusinessKey(context.getType(),
                context.getServiceId());

        if (product == null) {
            // 没有配置计费规则，直接放行
            return;
        }

        if (!product.isActive()) {
            throw new BusinessException("商品已被禁用，无法计费");
        }

        // 3. 检查幂等性
        if (context.getRequestId() != null && usageRecordDomainService.existsByRequestId(context.getRequestId())) {
            // 请求已处理，直接返回
            return;
        }

        // 4. 获取规则和策略
        RuleEntity rule = ruleDomainService.getRuleById(product.getRuleId());
        if (rule == null) {
            throw new BusinessException("关联的计费规则不存在");
        }

        RuleStrategy strategy = billingStrategyFactory.getStrategy(rule.getHandlerKey());

        // 5. 计算费用
        BigDecimal cost = strategy.process(context.getUsageData(), product.getPricingConfig());

        if (cost.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("计算出的费用不能为负数");
        }

        // 实现最低计费0.01元逻辑：如果费用大于0但小于0.01，则按0.01计算
        if (cost.compareTo(BigDecimal.ZERO) > 0 && cost.compareTo(new BigDecimal("0.01")) < 0) {
            cost = new BigDecimal("0.01");
        }

        // 如果费用为0，也需要记录用量，但不扣费
        if (cost.compareTo(BigDecimal.ZERO) == 0) {
            recordUsage(context, product, cost);
            return;
        }

        // 6. 检查余额并扣费
        accountDomainService.deduct(context.getUserId(), cost);

        // 7. 记录用量
        recordUsage(context, product, cost);
    }

    /** 检查余额是否充足（不实际扣费）
     * 
     * @param context 计费上下文
     * @return 是否余额充足 */
    public boolean checkBalance(RuleContext context) {
        try {
            // 查找商品
            ProductEntity product = productDomainService.findProductByBusinessKey(context.getType(),
                    context.getServiceId());

            if (product == null || !product.isActive()) {
                return true; // 无需计费
            }

            // 获取规则和策略
            RuleEntity rule = ruleDomainService.getRuleById(product.getRuleId());
            if (rule == null) {
                return false;
            }

            RuleStrategy strategy = billingStrategyFactory.getStrategy(rule.getHandlerKey());

            // 计算费用
            BigDecimal cost = strategy.process(context.getUsageData(), product.getPricingConfig());

            // 实现最低计费0.01元逻辑
            if (cost.compareTo(BigDecimal.ZERO) > 0 && cost.compareTo(new BigDecimal("0.01")) < 0) {
                cost = new BigDecimal("0.01");
            }

            // 允许为负数
            if (cost.compareTo(BigDecimal.ZERO) <= 0) {
                return true; // 无需扣费
            }

            // 检查余额
            return accountDomainService.checkSufficientBalance(context.getUserId(), cost);

        } catch (Exception e) {
            return false;
        }
    }

    /** 记录用量 */
    private void recordUsage(RuleContext context, ProductEntity product, BigDecimal cost) {
        // 获取业务信息
        Map<String, UsageRecordBusinessInfoService.BusinessInfo> businessInfoMap = businessInfoService
                .getBatchBusinessInfo(Set.of(product.getId()));
        UsageRecordBusinessInfoService.BusinessInfo businessInfo = businessInfoMap.get(product.getId());

        String serviceName = businessInfo != null ? businessInfo.getServiceName() : "未知服务";
        String serviceType = businessInfo != null ? businessInfo.getServiceType() : "未知类型";
        String serviceDescription = businessInfo != null ? businessInfo.getServiceDescription() : "";
        String pricingRule = businessInfo != null ? businessInfo.getPricingRule() : "";
        String relatedEntityName = businessInfo != null ? businessInfo.getRelatedEntityName() : "";

        // 使用新的创建方法，包含业务信息
        UsageRecordEntity usageRecord = UsageRecordEntity.createNewWithBusinessInfo(context.getUserId(),
                product.getId(), context.getUsageData(), cost, context.getRequestId(), serviceName, serviceType,
                serviceDescription, pricingRule, relatedEntityName);
        usageRecord.setId(UUID.randomUUID().toString());

        usageRecordDomainService.createUsageRecord(usageRecord);
    }
}