package org.xhy.domain.rule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.xhy.domain.rule.constant.RuleHandlerKey;
import org.xhy.domain.rule.model.RuleEntity;
import org.xhy.domain.rule.repository.RuleRepository;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.List;

/** 规则领域服务 处理计费规则相关的核心业务逻辑 */
@Service
public class RuleDomainService {

    private final RuleRepository ruleRepository;

    public RuleDomainService(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    /** 根据ID获取规则
     * @param ruleId 规则ID
     * @return 规则实体，如果不存在则返回null */
    public RuleEntity getRuleById(String ruleId) {
        if (ruleId == null || ruleId.trim().isEmpty()) {
            return null;
        }
        return ruleRepository.selectById(ruleId);
    }

    /** 根据处理器标识获取规则
     * @param handlerKey 处理器标识
     * @return 规则实体，如果不存在则返回null */
    public RuleEntity getRuleByHandlerKey(String handlerKey) {
        if (handlerKey == null || handlerKey.trim().isEmpty()) {
            return null;
        }

        RuleHandlerKey ruleHandlerKey;
        try {
            ruleHandlerKey = RuleHandlerKey.fromKey(handlerKey);
        } catch (IllegalArgumentException e) {
            return null;
        }

        LambdaQueryWrapper<RuleEntity> wrapper = Wrappers.<RuleEntity>lambdaQuery().eq(RuleEntity::getHandlerKey,
                ruleHandlerKey);

        return ruleRepository.selectOne(wrapper);
    }

    /** 创建规则
     * @param rule 规则实体
     * @return 创建后的规则实体 */
    public RuleEntity createRule(RuleEntity rule) {
        // 验证规则信息
        rule.validate();

        // 检查处理器标识是否已存在
        RuleEntity existing = getRuleByHandlerKey(rule.getHandlerKey().getKey());
        if (existing != null) {
            throw new BusinessException("该处理器标识的规则已存在: " + rule.getHandlerKey());
        }

        ruleRepository.insert(rule);
        return rule;
    }

    /** 更新规则
     * @param rule 规则实体
     * @return 更新后的规则实体 */
    public RuleEntity updateRule(RuleEntity rule) {
        if (rule.getId() == null || rule.getId().trim().isEmpty()) {
            throw new BusinessException("规则ID不能为空");
        }

        // 验证规则信息
        rule.validate();

        // 检查是否存在
        RuleEntity existing = getRuleById(rule.getId());
        if (existing == null) {
            throw new BusinessException("规则不存在");
        }

        // 检查处理器标识是否与其他规则冲突
        RuleEntity conflictRule = getRuleByHandlerKey(rule.getHandlerKey().getKey());
        if (conflictRule != null && !conflictRule.getId().equals(rule.getId())) {
            throw new BusinessException("该处理器标识的规则已存在: " + rule.getHandlerKey());
        }

        ruleRepository.checkedUpdateById(rule);
        return rule;
    }

    /** 删除规则（软删除）
     * @param ruleId 规则ID */
    public void deleteRule(String ruleId) {
        RuleEntity rule = getRuleById(ruleId);
        if (rule == null) {
            throw new BusinessException("规则不存在");
        }

        ruleRepository.deleteById(ruleId);
    }

    /** 获取所有规则
     * @return 规则列表 */
    public List<RuleEntity> getAllRules() {
        LambdaQueryWrapper<RuleEntity> wrapper = Wrappers.<RuleEntity>lambdaQuery()
                .orderByDesc(RuleEntity::getCreatedAt);

        return ruleRepository.selectList(wrapper);
    }

    /** 检查规则是否存在
     * @param ruleId 规则ID
     * @return 是否存在 */
    public boolean existsRule(String ruleId) {
        return getRuleById(ruleId) != null;
    }

    /** 检查处理器标识是否存在
     * @param handlerKey 处理器标识
     * @return 是否存在 */
    public boolean existsByHandlerKey(String handlerKey) {
        return getRuleByHandlerKey(handlerKey) != null;
    }

    public IPage page(Integer page, Integer pageSize) {
        return ruleRepository.selectPage(new Page<>(page, pageSize), null);
    }
}