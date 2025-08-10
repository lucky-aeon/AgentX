package org.xhy.application.account.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.account.assembler.AccountAssembler;
import org.xhy.application.account.dto.AccountDTO;
import org.xhy.domain.user.model.AccountEntity;
import org.xhy.domain.user.service.AccountDomainService;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.interfaces.dto.account.request.AddCreditRequest;
import org.xhy.interfaces.dto.account.request.RechargeRequest;

import java.math.BigDecimal;

/** 账户应用服务 处理账户相关的业务流程编排 */
@Service
public class AccountAppService {

    private final AccountDomainService accountDomainService;

    public AccountAppService(AccountDomainService accountDomainService) {
        this.accountDomainService = accountDomainService;
    }

    /** 获取用户账户信息
     * @param userId 用户ID
     * @return 账户DTO */
    public AccountDTO getUserAccount(String userId) {
        AccountEntity entity = accountDomainService.getOrCreateAccount(userId);
        return AccountAssembler.toDTO(entity);
    }

    /** 根据账户ID获取账户信息
     * @param accountId 账户ID
     * @return 账户DTO */
    public AccountDTO getAccountById(String accountId) {
        AccountEntity entity = accountDomainService.getAccountById(accountId);
        if (entity == null) {
            throw new BusinessException("账户不存在");
        }
        return AccountAssembler.toDTO(entity);
    }

    /** 账户充值
     * @param userId 用户ID
     * @param amount 充值金额
     * @return 充值后的账户信息 */
    @Transactional
    public AccountDTO recharge(String userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额必须大于0");
        }

        // 执行充值
        accountDomainService.rechargeBalance(userId, amount);

        // 返回更新后的账户信息
        AccountEntity updatedEntity = accountDomainService.getAccountByUserId(userId);
        return AccountAssembler.toDTO(updatedEntity);
    }

    /** 增加信用额度
     * @param userId 用户ID
     * @param request 增加信用额度请求
     * @return 更新后的账户信息 */
    @Transactional
    public AccountDTO addCredit(String userId, AddCreditRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("信用额度必须大于0");
        }

        // 增加信用额度
        accountDomainService.addCredit(userId, request.getAmount());

        // 返回更新后的账户信息
        AccountEntity updatedEntity = accountDomainService.getAccountByUserId(userId);
        return AccountAssembler.toDTO(updatedEntity);
    }

    /** 检查账户余额是否充足
     * @param userId 用户ID
     * @param amount 需要检查的金额
     * @return 是否充足 */
    public boolean checkSufficientBalance(String userId, BigDecimal amount) {
        return accountDomainService.checkSufficientBalance(userId, amount);
    }

    /** 获取账户可用余额
     * @param userId 用户ID
     * @return 可用余额 */
    public BigDecimal getAvailableBalance(String userId) {
        return accountDomainService.getAvailableBalance(userId);
    }

    /** 检查账户是否存在
     * @param userId 用户ID
     * @return 是否存在 */
    public boolean existsAccount(String userId) {
        return accountDomainService.existsAccount(userId);
    }
}