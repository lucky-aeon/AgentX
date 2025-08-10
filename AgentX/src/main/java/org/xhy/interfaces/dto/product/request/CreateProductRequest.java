package org.xhy.interfaces.dto.product.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/** 创建商品请求 */
public class CreateProductRequest {

    @NotBlank(message = "商品名称不能为空")
    private String name;

    @NotBlank(message = "商品类型不能为空")
    private String type;

    @NotBlank(message = "业务ID不能为空")
    private String serviceId;

    @NotBlank(message = "规则ID不能为空")
    private String ruleId;

    @NotNull(message = "价格配置不能为空")
    private Map<String, Object> pricingConfig;

    private Integer status = 1; // 默认激活

    public CreateProductRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
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
}