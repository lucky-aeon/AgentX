package org.xhy.application.billing.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.billing.dto.RuleContext;
import org.xhy.domain.product.constant.BillingType;
import org.xhy.domain.product.model.ProductEntity;
import org.xhy.domain.product.service.ProductDomainService;
import org.xhy.domain.rule.constant.RuleHandlerKey;
import org.xhy.domain.rule.model.RuleEntity;
import org.xhy.domain.rule.service.RuleDomainService;
import org.xhy.domain.user.model.AccountEntity;
import org.xhy.domain.user.service.AccountDomainService;
import org.xhy.infrastructure.billing.strategy.RuleStrategy;
import org.xhy.infrastructure.billing.strategy.BillingStrategyFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 计费系统集成测试 用于验证Product→Rule→Strategy完整链路 */
@SpringBootTest
@SpringJUnitConfig
@Transactional
public class BillingIntegrationTest {

    @Autowired
    private ProductDomainService productDomainService;

    @Autowired
    private RuleDomainService ruleDomainService;

    @Autowired
    private AccountDomainService accountDomainService;

    @Autowired
    private BillingStrategyFactory billingStrategyFactory;

    @Autowired
    private BillingService billingService;

    /** 测试模型Token计费链路 创建Rule→创建Product→执行计费→验证结果 */
    @Test
    public void testModelTokenBillingChain() {
        System.out.println("=== 开始测试模型Token计费链路 ===");

        // 1. 创建Token计费规则
        RuleEntity tokenRule = new RuleEntity();
        tokenRule.setId(UUID.randomUUID().toString());
        tokenRule.setName("测试模型Token计费规则");
        tokenRule.setHandlerKey(RuleHandlerKey.MODEL_TOKEN_STRATEGY);
        tokenRule.setDescription("用于测试的模型Token计费规则");

        RuleEntity createdRule = ruleDomainService.createRule(tokenRule);
        assertNotNull(createdRule);
        System.out.println("✓ 创建规则成功: " + createdRule.getName());

        // 2. 创建模型商品
        ProductEntity modelProduct = new ProductEntity();
        modelProduct.setId(UUID.randomUUID().toString());
        modelProduct.setName("测试GPT-4模型");
        modelProduct.setType(BillingType.MODEL_USAGE);
        modelProduct.setServiceId("test-gpt-4");
        modelProduct.setRuleId(createdRule.getId());
        modelProduct.activate();

        // 设置价格配置：输入5.0元/百万token，输出15.0元/百万token
        Map<String, Object> pricingConfig = new HashMap<>();
        pricingConfig.put("input_cost_per_million", 5.0);
        pricingConfig.put("output_cost_per_million", 15.0);
        modelProduct.setPricingConfig(pricingConfig);

        ProductEntity createdProduct = productDomainService.createProduct(modelProduct);
        assertNotNull(createdProduct);
        System.out.println("✓ 创建商品成功: " + createdProduct.getName());

        // 3. 测试策略工厂
        RuleStrategy strategy = billingStrategyFactory.getStrategy(RuleHandlerKey.MODEL_TOKEN_STRATEGY);
        assertNotNull(strategy);
        assertEquals("MODEL_TOKEN_STRATEGY", strategy.getStrategyName());
        System.out.println("✓ 获取策略成功: " + strategy.getStrategyName());

        // 4. 测试费用计算
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("input", 1000); // 1000个输入token
        usageData.put("output", 500); // 500个输出token

        BigDecimal cost = strategy.process(usageData, pricingConfig);
        assertNotNull(cost);
        System.out.println("✓ 费用计算成功: " + cost + " 元");

        // 期望费用：(1000/1000000)*5.0 + (500/1000000)*15.0 = 0.005 + 0.0075 = 0.0125
        BigDecimal expectedCost = new BigDecimal("0.01250000");
        assertEquals(0, cost.compareTo(expectedCost), "费用计算应该正确");
        System.out.println("✓ 费用计算正确");

        // 5. 测试完整计费流程
        String testUserId = "test-user-" + System.currentTimeMillis();

        // 创建测试账户并充值
        AccountEntity account = AccountEntity.createNew(testUserId);
        account.setBalance(new BigDecimal("100.00"));
        account.setCredit(BigDecimal.ZERO);
        account.setTotalConsumed(BigDecimal.ZERO);
        accountDomainService.createAccount(account);
        System.out.println("✓ 创建测试账户成功，余额: " + account.getBalance());

        // 执行计费
        RuleContext context = RuleContext.builder().type(BillingType.MODEL_USAGE.getCode()).serviceId("test-gpt-4")
                .usageData(usageData).requestId("test-request-" + System.currentTimeMillis()).userId(testUserId)
                .build();

        assertDoesNotThrow(() -> billingService.charge(context));
        System.out.println("✓ 计费执行成功");

        // 验证余额变化
        AccountEntity updatedAccount = accountDomainService.getAccountByUserId(testUserId);
        BigDecimal remainingBalance = new BigDecimal("100.00").subtract(cost);
        assertEquals(0, updatedAccount.getBalance().compareTo(remainingBalance), "余额扣减应该正确");
        System.out.println("✓ 余额扣减正确，剩余: " + updatedAccount.getBalance());

        System.out.println("=== 模型Token计费链路测试完成 ===\n");
    }

    /** 测试按次计费链路 */
    @Test
    public void testPerUnitBillingChain() {
        System.out.println("=== 开始测试按次计费链路 ===");

        // 1. 创建按次计费规则
        RuleEntity perUnitRule = new RuleEntity();
        perUnitRule.setId(UUID.randomUUID().toString());
        perUnitRule.setName("测试按次计费规则");
        perUnitRule.setHandlerKey(RuleHandlerKey.PER_UNIT_STRATEGY);
        perUnitRule.setDescription("用于测试的按次计费规则");

        RuleEntity createdRule = ruleDomainService.createRule(perUnitRule);
        assertNotNull(createdRule);
        System.out.println("✓ 创建规则成功: " + createdRule.getName());

        // 2. 创建Agent创建商品
        ProductEntity agentProduct = new ProductEntity();
        agentProduct.setId(UUID.randomUUID().toString());
        agentProduct.setName("测试Agent创建费用");
        agentProduct.setType(BillingType.AGENT_CREATION);
        agentProduct.setServiceId("agent_creation");
        agentProduct.setRuleId(createdRule.getId());
        agentProduct.activate();

        // 设置价格配置：每次10.0元
        Map<String, Object> pricingConfig = new HashMap<>();
        pricingConfig.put("cost_per_unit", 10.0);
        agentProduct.setPricingConfig(pricingConfig);

        ProductEntity createdProduct = productDomainService.createProduct(agentProduct);
        assertNotNull(createdProduct);
        System.out.println("✓ 创建商品成功: " + createdProduct.getName());

        // 3. 测试策略
        RuleStrategy strategy = billingStrategyFactory.getStrategy(RuleHandlerKey.PER_UNIT_STRATEGY);
        assertNotNull(strategy);
        assertEquals("PER_UNIT_STRATEGY", strategy.getStrategyName());
        System.out.println("✓ 获取策略成功: " + strategy.getStrategyName());

        // 4. 测试费用计算
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("quantity", 1); // 创建1个Agent

        BigDecimal cost = strategy.process(usageData, pricingConfig);
        assertNotNull(cost);
        System.out.println("✓ 费用计算成功: " + cost + " 元");

        // 期望费用：1 * 10.0 = 10.0
        BigDecimal expectedCost = new BigDecimal("10.00000000");
        assertEquals(0, cost.compareTo(expectedCost), "费用计算应该正确");
        System.out.println("✓ 费用计算正确");

        System.out.println("=== 按次计费链路测试完成 ===\n");
    }

    /** 测试余额不足场景 */
    @Test
    public void testInsufficientBalance() {
        System.out.println("=== 开始测试余额不足场景 ===");

        // 创建规则和商品
        RuleEntity rule = new RuleEntity();
        rule.setId(UUID.randomUUID().toString());
        rule.setName("余额不足测试规则");
        rule.setHandlerKey(RuleHandlerKey.PER_UNIT_STRATEGY);
        rule.setDescription("用于测试余额不足的规则");

        RuleEntity createdRule = ruleDomainService.createRule(rule);

        ProductEntity product = new ProductEntity();
        product.setId(UUID.randomUUID().toString());
        product.setName("高价商品");
        product.setType(BillingType.AGENT_CREATION);
        product.setServiceId("expensive_service");
        product.setRuleId(createdRule.getId());
        product.activate();

        Map<String, Object> pricingConfig = new HashMap<>();
        pricingConfig.put("cost_per_unit", 1000.0); // 很高的价格
        product.setPricingConfig(pricingConfig);

        productDomainService.createProduct(product);

        // 创建余额不足的账户
        String testUserId = "poor-user-" + System.currentTimeMillis();
        AccountEntity account = AccountEntity.createNew(testUserId);
        account.setBalance(new BigDecimal("1.00")); // 余额很少
        account.setCredit(BigDecimal.ZERO);
        account.setTotalConsumed(BigDecimal.ZERO);
        accountDomainService.createAccount(account);

        // 尝试计费，应该抛出异常
        RuleContext context = RuleContext.builder().type(BillingType.AGENT_CREATION.getCode())
                .serviceId("expensive_service").usageData(Map.of("quantity", 1))
                .requestId("insufficient-balance-test-" + System.currentTimeMillis()).userId(testUserId).build();

        assertThrows(Exception.class, () -> billingService.charge(context), "余额不足应该抛出异常");
        System.out.println("✓ 余额不足正确抛出异常");

        System.out.println("=== 余额不足场景测试完成 ===\n");
    }
}