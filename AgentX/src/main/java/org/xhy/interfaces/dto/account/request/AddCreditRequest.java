package org.xhy.interfaces.dto.account.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** 增加信用额度请求 */
public class AddCreditRequest {

    /** 增加的信用额度 */
    @NotNull(message = "信用额度不能为空")
    @DecimalMin(value = "0.01", message = "信用额度必须大于0.01")
    private BigDecimal amount;

    /** 备注信息 */
    private String remark;

    public AddCreditRequest() {
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}