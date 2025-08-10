package org.xhy.application.rule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.xhy.interfaces.dto.Page;
import org.xhy.interfaces.dto.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.rule.assembler.RuleAssembler;
import org.xhy.application.rule.dto.RuleDTO;
import org.xhy.domain.rule.model.RuleEntity;
import org.xhy.domain.rule.service.RuleDomainService;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.interfaces.dto.rule.request.CreateRuleRequest;
import org.xhy.interfaces.dto.rule.request.QueryRuleRequest;
import org.xhy.interfaces.dto.rule.request.UpdateRuleRequest;

import java.util.List;

/** 规则应用服务 处理规则相关的业务流程编排 */
@Service
public class RuleAppService {

    private final RuleDomainService ruleDomainService;

    public RuleAppService(RuleDomainService ruleDomainService) {
        this.ruleDomainService = ruleDomainService;
    }

    /** 创建规则
     * @param request 创建规则请求
     * @return 规则DTO */
    @Transactional
    public RuleDTO createRule(CreateRuleRequest request) {
        // 转换为实体并创建
        RuleEntity entity = RuleAssembler.toEntity(request);
        RuleEntity createdEntity = ruleDomainService.createRule(entity);

        return RuleAssembler.toDTO(createdEntity);
    }

    /** 更新规则
     * @param request 更新规则请求
     * @return 规则DTO */
    @Transactional
    public RuleDTO updateRule(UpdateRuleRequest request, String ruleId) {
        RuleEntity ruleEntity = RuleAssembler.toEntity(request, ruleId);

        RuleEntity updatedEntity = ruleDomainService.updateRule(ruleEntity);

        return RuleAssembler.toDTO(updatedEntity);
    }

    /** 根据ID获取规则
     * @param ruleId 规则ID
     * @return 规则DTO */
    public RuleDTO getRuleById(String ruleId) {
        RuleEntity entity = ruleDomainService.getRuleById(ruleId);
        if (entity == null) {
            throw new BusinessException("规则不存在");
        }
        return RuleAssembler.toDTO(entity);
    }

    /** 根据处理器标识获取规则
     * @param handlerKey 处理器标识
     * @return 规则DTO，如果不存在则返回null */
    public RuleDTO getRuleByHandlerKey(String handlerKey) {
        RuleEntity entity = ruleDomainService.getRuleByHandlerKey(handlerKey);
        return RuleAssembler.toDTO(entity);
    }

    /** 分页查询规则
     * @param request 查询请求
     * @return 规则分页结果 */
    public PageResult<RuleDTO> getRules(QueryRuleRequest request) {
        // 构建查询条件
        LambdaQueryWrapper<RuleEntity> wrapper = Wrappers.<RuleEntity>lambdaQuery()
                .eq(StringUtils.isNotBlank(request.getHandlerKey()), RuleEntity::getHandlerKey, request.getHandlerKey())
                .and(StringUtils.isNotBlank(request.getKeyword()), w -> {
                    String keyword = request.getKeyword().trim();
                    w.like(RuleEntity::getName, keyword).or().like(RuleEntity::getDescription, keyword);
                }).orderByDesc(RuleEntity::getCreatedAt);

        // 分页查询
        IPage page = ruleDomainService.page(request.getPage(), request.getPageSize());

        // 转换结果
        List<RuleDTO> dtoList = RuleAssembler.toDTOs(page.getRecords());

        PageResult<RuleDTO> resultPage = new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(dtoList);

        return resultPage;
    }

    /** 获取所有规则
     * @return 规则DTO列表 */
    public List<RuleDTO> getAllRules() {
        List<RuleEntity> entities = ruleDomainService.getAllRules();
        return RuleAssembler.toDTOs(entities);
    }

    /** 删除规则
     * @param ruleId 规则ID */
    @Transactional
    public void deleteRule(String ruleId) {
        ruleDomainService.deleteRule(ruleId);
    }

    /** 检查规则是否存在
     * @param ruleId 规则ID
     * @return 是否存在 */
    public boolean existsRule(String ruleId) {
        return ruleDomainService.existsRule(ruleId);
    }

    /** 检查处理器标识是否存在
     * @param handlerKey 处理器标识
     * @return 是否存在 */
    public boolean existsByHandlerKey(String handlerKey) {
        return ruleDomainService.existsByHandlerKey(handlerKey);
    }
}