package org.xhy.application.account.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 账户DTO 用户账户信息传输对象 */
public class AccountDTO {

    /** 账户ID */
    private String id;

    /** 用户ID */
    private String userId;

    /** 账户余额 */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
    private BigDecimal balance;

    /** 信用额度 */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
    private BigDecimal credit;

    /** 总消费金额 */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
    private BigDecimal totalConsumed;

    /** 最后交易时间 */
    private LocalDateTime lastTransactionAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    public AccountDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public BigDecimal getTotalConsumed() {
        return totalConsumed;
    }

    public void setTotalConsumed(BigDecimal totalConsumed) {
        this.totalConsumed = totalConsumed;
    }

    public LocalDateTime getLastTransactionAt() {
        return lastTransactionAt;
    }

    public void setLastTransactionAt(LocalDateTime lastTransactionAt) {
        this.lastTransactionAt = lastTransactionAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** 获取可用余额（余额+信用额度） */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
    public BigDecimal getAvailableBalance() {
        BigDecimal availableBalance = balance != null ? balance : BigDecimal.ZERO;
        BigDecimal creditAmount = credit != null ? credit : BigDecimal.ZERO;
        return availableBalance.add(creditAmount);
    }
}