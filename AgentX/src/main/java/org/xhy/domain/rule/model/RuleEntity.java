package org.xhy.domain.rule.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.xhy.domain.rule.constant.RuleHandlerKey;
import org.xhy.infrastructure.converter.RuleHandlerKeyConverter;
import org.xhy.infrastructure.entity.BaseEntity;
import org.xhy.infrastructure.exception.BusinessException;

/** 规则实体 定义计费规则类型，独立于任何业务 */
@TableName(value = "rules", autoResultMap = true)
public class RuleEntity extends BaseEntity {

    /** 规则唯一ID */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 规则名称 */
    @TableField("name")
    private String name;

    /** 规则处理器标识，映射到代码中的策略类 */
    @TableField(value = "handler_key", typeHandler = RuleHandlerKeyConverter.class)
    private RuleHandlerKey handlerKey;

    /** 规则描述 */
    @TableField("description")
    private String description;

    public RuleEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RuleHandlerKey getHandlerKey() {
        return handlerKey;
    }

    public void setHandlerKey(RuleHandlerKey handlerKey) {
        this.handlerKey = handlerKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** 验证规则信息 */
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("规则名称不能为空");
        }
        if (handlerKey == null) {
            throw new BusinessException("规则处理器标识不能为空");
        }
        // handlerKey已经是枚举类型，不需要再验证有效性
    }

    /** 检查规则是否有效 */
    public boolean isValid() {
        try {
            validate();
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    /** 获取策略类名（用于反射或工厂模式） */
    public String getStrategyClassName() {
        if (handlerKey == null) {
            return null;
        }
        // 将枚举key转换为策略类名
        // 例如: MODEL_TOKEN_STRATEGY -> ModelTokenStrategy
        String[] parts = handlerKey.getKey().split("_");
        StringBuilder className = new StringBuilder();
        for (String part : parts) {
            className.append(part.substring(0, 1).toUpperCase()).append(part.substring(1).toLowerCase());
        }
        return className.toString();
    }
}