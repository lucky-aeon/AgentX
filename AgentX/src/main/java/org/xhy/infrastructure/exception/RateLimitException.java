package org.xhy.infrastructure.exception;

/** 限流异常 */
public class RateLimitException extends BusinessException {

    public RateLimitException(String message) {
        super(message);
    }

    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}