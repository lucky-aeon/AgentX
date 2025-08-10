package org.xhy.domain.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.xhy.domain.user.model.AccountEntity;
import org.xhy.domain.user.repository.AccountRepository;
import org.xhy.infrastructure.exception.BusinessException;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/** 账户领域服务 处理用户账户相关的核心业务逻辑，使用JVM锁确保并发安全 */
@Service
public class AccountDomainService {

    private final AccountRepository accountRepository;

    /** 用户级别的锁，确保同一用户的账户操作串行化 */
    private final ConcurrentHashMap<String, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    public AccountDomainService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /** 获取用户的锁对象
     * @param userId 用户ID
     * @return 锁对象 */
    private ReentrantLock getUserLock(String userId) {
        return userLocks.computeIfAbsent(userId, k -> new ReentrantLock());
    }

    /** 根据用户ID查找账户（私有方法）
     * @param userId 用户ID
     * @return 账户实体，如果不存在则返回null */
    private AccountEntity findAccountByUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<AccountEntity> wrapper = Wrappers.<AccountEntity>lambdaQuery().eq(AccountEntity::getUserId,
                userId);

        return accountRepository.selectOne(wrapper);
    }

    /** 获取或创建用户账户
     * @param userId 用户ID
     * @return 账户实体 */
    public AccountEntity getOrCreateAccount(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new BusinessException("用户ID不能为空");
        }

        AccountEntity account = findAccountByUserId(userId);
        if (account == null) {
            // 使用锁确保同一用户只创建一个账户
            ReentrantLock lock = getUserLock(userId);
            lock.lock();
            try {
                // 双重检查
                account = findAccountByUserId(userId);
                if (account == null) {
                    account = AccountEntity.createNew(userId);
                    accountRepository.insert(account);
                }
            } finally {
                lock.unlock();
            }
        }
        return account;
    }

    /** 根据用户ID获取账户
     * @param userId 用户ID
     * @return 账户实体，如果不存在则返回null */
    public AccountEntity getAccountByUserId(String userId) {
        return findAccountByUserId(userId);
    }

    /** 扣除账户余额（带锁保护）
     * @param userId 用户ID
     * @param amount 扣除金额
     * @throws BusinessException 余额不足或其他业务异常 */
    public void deductBalance(String userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("扣费金额必须大于0");
        }

        ReentrantLock lock = getUserLock(userId);
        lock.lock();
        try {
            // 获取最新的账户信息
            AccountEntity account = getOrCreateAccount(userId);

            // 扣除余额
            account.deduct(amount);

            // 更新数据库
            accountRepository.checkedUpdateById(account);

        } finally {
            lock.unlock();
        }
    }

    /** 账户扣费（deductBalance的别名）
     * @param userId 用户ID
     * @param amount 扣费金额 */
    public void deduct(String userId, BigDecimal amount) {
        deductBalance(userId, amount);
    }

    /** 账户充值（带锁保护）
     * @param userId 用户ID
     * @param amount 充值金额 */
    public void rechargeBalance(String userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额必须大于0");
        }

        ReentrantLock lock = getUserLock(userId);
        lock.lock();
        try {
            // 获取或创建账户
            AccountEntity account = getOrCreateAccount(userId);

            // 充值
            account.recharge(amount);

            // 更新数据库
            accountRepository.checkedUpdateById(account);

        } finally {
            lock.unlock();
        }
    }

    /** 增加信用额度（带锁保护）
     * @param userId 用户ID
     * @param amount 增加的信用额度 */
    public void addCredit(String userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("信用额度必须大于0");
        }

        ReentrantLock lock = getUserLock(userId);
        lock.lock();
        try {
            // 获取或创建账户
            AccountEntity account = getOrCreateAccount(userId);

            // 增加信用额度
            account.addCredit(amount);

            // 更新数据库
            accountRepository.checkedUpdateById(account);

        } finally {
            lock.unlock();
        }
    }

    /** 检查账户余额是否充足
     * @param userId 用户ID
     * @param amount 需要检查的金额
     * @return 是否充足 */
    public boolean checkSufficientBalance(String userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("检查金额必须大于等于0");
        }

        AccountEntity account = getAccountByUserId(userId);
        if (account == null) {
            return false;
        }

        return account.checkSufficientBalance(amount);
    }

    /** 获取账户可用余额
     * @param userId 用户ID
     * @return 可用余额 */
    public BigDecimal getAvailableBalance(String userId) {
        AccountEntity account = getAccountByUserId(userId);
        if (account == null) {
            return BigDecimal.ZERO;
        }
        return account.getAvailableBalance();
    }

    /** 检查账户是否存在
     * @param userId 用户ID
     * @return 是否存在 */
    public boolean existsAccount(String userId) {
        return findAccountByUserId(userId) != null;
    }

    /** 根据ID获取账户
     * @param accountId 账户ID
     * @return 账户实体，如果不存在则返回null */
    public AccountEntity getAccountById(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return null;
        }
        return accountRepository.selectById(accountId);
    }

    /** 更新账户信息
     * @param account 账户实体
     * @return 更新后的账户实体 */
    public AccountEntity updateAccount(AccountEntity account) {
        if (account == null || account.getId() == null) {
            throw new BusinessException("账户信息不能为空");
        }

        account.validate();

        ReentrantLock lock = getUserLock(account.getUserId());
        lock.lock();
        try {
            accountRepository.checkedUpdateById(account);
            return account;
        } finally {
            lock.unlock();
        }
    }

    /** 创建账户（用于测试）
     * @param account 账户实体
     * @return 创建后的账户实体 */
    public AccountEntity createAccount(AccountEntity account) {
        if (account == null) {
            throw new BusinessException("账户信息不能为空");
        }

        account.validate();

        ReentrantLock lock = getUserLock(account.getUserId());
        lock.lock();
        try {
            // 检查是否已存在
            AccountEntity existing = findAccountByUserId(account.getUserId());
            if (existing != null) {
                throw new BusinessException("用户账户已存在");
            }

            accountRepository.insert(account);
            return account;
        } finally {
            lock.unlock();
        }
    }
}