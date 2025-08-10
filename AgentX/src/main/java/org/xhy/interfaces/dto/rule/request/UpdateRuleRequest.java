package org.xhy.interfaces.dto.rule.request;

import jakarta.validation.constraints.NotBlank;

/** 更新规则请求 */
public class UpdateRuleRequest {

    @NotBlank(message = "规则名称不能为空")
    private String name;

    @NotBlank(message = "规则处理器不能为空")
    private String handlerKey;

    private String description;

    public UpdateRuleRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHandlerKey() {
        return handlerKey;
    }

    public void setHandlerKey(String handlerKey) {
        this.handlerKey = handlerKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}