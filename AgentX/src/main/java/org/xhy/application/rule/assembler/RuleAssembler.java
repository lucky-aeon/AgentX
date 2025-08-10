package org.xhy.application.rule.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.rule.dto.RuleDTO;
import org.xhy.domain.rule.constant.RuleHandlerKey;
import org.xhy.domain.rule.model.RuleEntity;
import org.xhy.interfaces.dto.rule.request.CreateRuleRequest;
import org.xhy.interfaces.dto.rule.request.UpdateRuleRequest;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** 规则装配器 处理Entity、DTO、Request之间的转换 */
public class RuleAssembler {

    /** 将Entity转换为DTO
     * @param entity 规则实体
     * @return 规则DTO */
    public static RuleDTO toDTO(RuleEntity entity) {
        if (entity == null) {
            return null;
        }

        RuleDTO dto = new RuleDTO();
        BeanUtils.copyProperties(entity, dto, "handlerKey"); // 排除handlerKey字段

        // 手动设置handlerKey字段
        if (entity.getHandlerKey() != null) {
            dto.setHandlerKey(entity.getHandlerKey().getKey());
        }

        return dto;
    }

    /** 将Entity列表转换为DTO列表
     * @param entities 规则实体列表
     * @return 规则DTO列表 */
    public static List<RuleDTO> toDTOs(List<RuleEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(RuleAssembler::toDTO).collect(Collectors.toList());
    }

    /** 将创建请求转换为Entity
     * @param request 创建规则请求
     * @return 规则实体 */
    public static RuleEntity toEntity(CreateRuleRequest request) {
        if (request == null) {
            return null;
        }

        RuleEntity entity = new RuleEntity();
        BeanUtils.copyProperties(request, entity, "handlerKey"); // 排除handlerKey字段

        // 手动设置handlerKey字段
        if (request.getHandlerKey() != null) {
            entity.setHandlerKey(RuleHandlerKey.fromKey(request.getHandlerKey()));
        }

        return entity;
    }

    /** 将更新请求转换为Entity
     * @param request 更新规则请求
     * @return 规则实体 */
    public static RuleEntity toEntity(UpdateRuleRequest request) {
        if (request == null) {
            return null;
        }

        RuleEntity entity = new RuleEntity();
        BeanUtils.copyProperties(request, entity);
        return entity;
    }

    /** 更新Entity的字段（从更新请求）
     * @param entity 目标实体
     * @param request 更新请求 */
    public static void updateEntity(RuleEntity entity, UpdateRuleRequest request) {
        if (entity == null || request == null) {
            return;
        }

        // 只更新非空字段
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getHandlerKey() != null) {
            entity.setHandlerKey(RuleHandlerKey.fromKey(request.getHandlerKey()));
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
    }

    public static RuleEntity toEntity(UpdateRuleRequest request, String ruleId) {
        RuleEntity entity = new RuleEntity();
        BeanUtils.copyProperties(request, entity);
        entity.setId(ruleId);
        entity.setHandlerKey(RuleHandlerKey.fromKey(request.getHandlerKey()));
        return entity;
    }
}