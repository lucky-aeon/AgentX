package org.xhy.infrastructure.exception;

/** 余额不足异常 当用户账户余额不足以支付计费时抛出 */
public class InsufficientBalanceException extends BusinessException {

    /** 构造余额不足异常
     * 
     * @param message 异常消息 */
    public InsufficientBalanceException(String message) {
        super(message);
    }

    /** 构造余额不足异常
     * 
     * @param message 异常消息
     * @param cause 原因异常 */
    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}