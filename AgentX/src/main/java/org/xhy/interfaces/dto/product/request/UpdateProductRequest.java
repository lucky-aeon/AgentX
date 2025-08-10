package org.xhy.interfaces.dto.product.request;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/** 更新商品请求 */
public class UpdateProductRequest {

    @NotBlank(message = "商品名称不能为空")
    private String name;

    @NotBlank(message = "商品类型不能为空")
    private String type;

    @NotBlank(message = "服务ID不能为空")
    private String serviceId;

    @NotBlank(message = "规则ID不能为空")
    private String ruleId;

    private Map<String, Object> pricingConfig;

    private Integer status;

    public UpdateProductRequest() {
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