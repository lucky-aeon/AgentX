package org.xhy.domain.product.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.xhy.domain.product.constant.BillingType;
import org.xhy.infrastructure.converter.BillingTypeConverter;
import org.xhy.infrastructure.converter.PricingConfigConverter;
import org.xhy.infrastructure.entity.BaseEntity;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.Map;

/** 商品实体 定义可计费项，关联业务，存储价格配置 */
@TableName(value = "products", autoResultMap = true)
public class ProductEntity extends BaseEntity {

    /** 商品唯一ID */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 商品名称 */
    @TableField("name")
    private String name;

    /** 商品类型 (MODEL_USAGE, AGENT_CREATION等) */
    @TableField(value = "type", typeHandler = BillingTypeConverter.class)
    private BillingType type;

    /** 关联的业务ID */
    @TableField("service_id")
    private String serviceId;

    /** 关联的规则ID */
    @TableField("rule_id")
    private String ruleId;

    /** 价格配置 JSON格式 */
    @TableField(value = "pricing_config", typeHandler = PricingConfigConverter.class)
    private Map<String, Object> pricingConfig;

    /** 状态 1-激活 0-禁用 */
    @TableField("status")
    private Integer status;

    public ProductEntity() {
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

    public BillingType getType() {
        return type;
    }

    public void setType(BillingType type) {
        this.type = type;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public Map<String, Object> getPricingConfig() {
        return pricingConfig;
    }

    public void setPricingConfig(Map<String, Object> pricingConfig) {
        this.pricingConfig = pricingConfig;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    /** 检查商品是否激活 */
    public boolean isActive() {
        return Integer.valueOf(1).equals(status);
    }

    /** 激活商品 */
    public void activate() {
        this.status = 1;
    }

    /** 禁用商品 */
    public void deactivate() {
        this.status = 0;
    }

    /** 更新状态 */
    public void updateStatus(Integer newStatus) {
        if (newStatus == null || (newStatus != 0 && newStatus != 1)) {
            throw new BusinessException("商品状态只能为0(禁用)或1(激活)");
        }
        this.status = newStatus;
    }

    /** 验证价格配置 */
    public void validatePricingConfig() {
        if (pricingConfig == null || pricingConfig.isEmpty()) {
            throw new BusinessException("商品价格配置不能为空");
        }
    }

    /** 验证商品基本信息 */
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("商品名称不能为空");
        }
        if (type == null) {
            throw new BusinessException("商品类型不能为空");
        }
        if (serviceId == null || serviceId.trim().isEmpty()) {
            throw new BusinessException("业务ID不能为空");
        }
        if (ruleId == null || ruleId.trim().isEmpty()) {
            throw new BusinessException("规则ID不能为空");
        }
        validatePricingConfig();
    }
}