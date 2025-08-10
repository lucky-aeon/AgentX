package org.xhy.interfaces.api.admin.rule;

import org.xhy.interfaces.dto.PageResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.rule.dto.RuleDTO;
import org.xhy.application.rule.service.RuleAppService;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.rule.request.CreateRuleRequest;
import org.xhy.interfaces.dto.rule.request.QueryRuleRequest;
import org.xhy.interfaces.dto.rule.request.UpdateRuleRequest;

import java.util.List;

/** 计费规则控制层 提供规则管理的API接口 */
@RestController
@RequestMapping("/admin/rules")
public class AdminRuleController {

    private final RuleAppService ruleAppService;

    public AdminRuleController(RuleAppService ruleAppService) {
        this.ruleAppService = ruleAppService;
    }

    /** 创建计费规则
     * 
     * @param request 创建规则请求
     * @return 创建的规则信息 */
    @PostMapping
    public Result<RuleDTO> createRule(@RequestBody @Validated CreateRuleRequest request) {
        RuleDTO rule = ruleAppService.createRule(request);
        return Result.success(rule);
    }

    /** 更新计费规则
     * 
     * @param request 更新规则请求
     * @return 更新后的规则信息 */
    @PutMapping("/{ruleId}")
    public Result<RuleDTO> updateRule(@RequestBody @Validated UpdateRuleRequest request, @PathVariable String ruleId) {
        RuleDTO rule = ruleAppService.updateRule(request, ruleId);
        return Result.success(rule);
    }

    /** 根据ID获取计费规则
     * 
     * @param ruleId 规则ID
     * @return 规则信息 */
    @GetMapping("/{ruleId}")
    public Result<RuleDTO> getRuleById(@PathVariable String ruleId) {
        RuleDTO rule = ruleAppService.getRuleById(ruleId);
        return Result.success(rule);
    }

    /** 根据处理器标识获取规则
     * 
     * @param handlerKey 处理器标识
     * @return 规则信息 */
    @GetMapping("/handler/{handlerKey}")
    public Result<RuleDTO> getRuleByHandlerKey(@PathVariable String handlerKey) {
        RuleDTO rule = ruleAppService.getRuleByHandlerKey(handlerKey);
        return Result.success(rule);
    }

    /** 分页查询计费规则
     * 
     * @param request 查询参数
     * @return 规则分页列表 */
    @GetMapping
    public Result<PageResult<RuleDTO>> getRules(QueryRuleRequest request) {
        PageResult<RuleDTO> rules = ruleAppService.getRules(request);
        return Result.success(rules);
    }

    /** 获取所有计费规则
     * 
     * @return 所有规则列表 */
    @GetMapping("/all")
    public Result<List<RuleDTO>> getAllRules() {
        List<RuleDTO> rules = ruleAppService.getAllRules();
        return Result.success(rules);
    }

    /** 删除计费规则
     * 
     * @param ruleId 规则ID
     * @return 删除结果 */
    @DeleteMapping("/{ruleId}")
    public Result<Void> deleteRule(@PathVariable String ruleId) {
        ruleAppService.deleteRule(ruleId);
        return Result.success();
    }

    /** 检查规则是否存在
     * 
     * @param ruleId 规则ID
     * @return 是否存在 */
    @GetMapping("/{ruleId}/exists")
    public Result<Boolean> existsRule(@PathVariable String ruleId) {
        boolean exists = ruleAppService.existsRule(ruleId);
        return Result.success(exists);
    }

    /** 检查处理器标识是否存在
     * 
     * @param handlerKey 处理器标识
     * @return 是否存在 */
    @GetMapping("/handler/{handlerKey}/exists")
    public Result<Boolean> existsByHandlerKey(@PathVariable String handlerKey) {
        boolean exists = ruleAppService.existsByHandlerKey(handlerKey);
        return Result.success(exists);
    }
}