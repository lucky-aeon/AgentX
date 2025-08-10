package org.xhy.domain.user.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.xhy.infrastructure.entity.BaseEntity;
import org.xhy.infrastructure.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 账户实体 管理用户的资金余额信息 */
@TableName(value = "accounts", autoResultMap = true)
public class AccountEntity extends BaseEntity {

    /** 账户唯一ID */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 用户ID */
    @TableField("user_id")
    private String userId;

    /** 账户余额 */
    @TableField("balance")
    private BigDecimal balance;

    /** 信用额度/赠送额度 */
    @TableField("credit")
    private BigDecimal credit;

    /** 累计消费金额 */
    @TableField("total_consumed")
    private BigDecimal totalConsumed;

    /** 最后交易时间 */
    @TableField("last_transaction_at")
    private LocalDateTime lastTransactionAt;

    public AccountEntity() {
        this.balance = BigDecimal.ZERO;
        this.credit = BigDecimal.ZERO;
        this.totalConsumed = BigDecimal.ZERO;
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

    /** 获取可用余额（余额 + 信用额度） */
    public BigDecimal getAvailableBalance() {
        return balance.add(credit);
    }

    /** 检查余额是否充足
     * @param amount 需要扣除的金额
     * @return 是否充足 */
    public boolean checkSufficientBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("扣费金额必须大于0");
        }
        return getAvailableBalance().compareTo(amount) >= 0;
    }

    /** 扣除余额
     * @param amount 扣除金额
     * @throws BusinessException 余额不足时抛出异常 */
    public void deduct(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("扣费金额必须大于0");
        }

        if (!checkSufficientBalance(amount)) {
            throw new BusinessException("账户余额不足");
        }

        // 优先使用余额，不足时使用信用额度
        if (balance.compareTo(amount) >= 0) {
            // 余额充足，直接扣除
            this.balance = balance.subtract(amount);
        } else {
            // 余额不足，使用余额+信用额度
            BigDecimal remainingAmount = amount.subtract(balance);
            this.balance = BigDecimal.ZERO;
            this.credit = credit.subtract(remainingAmount);
        }

        // 更新累计消费和最后交易时间
        this.totalConsumed = totalConsumed.add(amount);
        this.lastTransactionAt = LocalDateTime.now();
    }

    /** 充值
     * @param amount 充值金额 */
    public void recharge(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额必须大于0");
        }

        this.balance = balance.add(amount);
        this.lastTransactionAt = LocalDateTime.now();
    }

    /** 增加信用额度
     * @param amount 增加的信用额度 */
    public void addCredit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("信用额度必须大于0");
        }

        this.credit = credit.add(amount);
        this.lastTransactionAt = LocalDateTime.now();
    }

    /** 验证账户信息 */
    public void validate() {
        if (userId == null || userId.trim().isEmpty()) {
            throw new BusinessException("用户ID不能为空");
        }
        if (balance == null) {
            this.balance = BigDecimal.ZERO;
        }
        if (credit == null) {
            this.credit = BigDecimal.ZERO;
        }
        if (totalConsumed == null) {
            this.totalConsumed = BigDecimal.ZERO;
        }
    }

    /** 创建新账户
     * @param userId 用户ID
     * @return 账户实体 */
    public static AccountEntity createNew(String userId) {
        AccountEntity account = new AccountEntity();
        account.setUserId(userId);
        account.validate();
        return account;
    }
}