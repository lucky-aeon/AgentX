package org.xhy.infrastructure.util;

import org.xhy.infrastructure.exception.ParamValidationException;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * 参数校验工具类
 */
public class ValidationUtils {
    
    // 简化版语义化版本格式，例如 1.0.0
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");
    
    /**
     * 校验参数不为空
     */
    public static void notNull(Object value, String paramName) {
        if (value == null) {
            throw new ParamValidationException(paramName, "不能为空");
        }
    }
    
    /**
     * 校验字符串不为空
     */
    public static void notEmpty(String value, String paramName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ParamValidationException(paramName, "不能为空");
        }
    }
    
    /**
     * 校验集合不为空
     */
    public static void notEmpty(Collection<?> collection, String paramName) {
        if (collection == null || collection.isEmpty()) {
            throw new ParamValidationException(paramName, "不能为空");
        }
    }
    
    /**
     * 校验字符串长度
     */
    public static void length(String value, int min, int max, String paramName) {
        if (value == null) {
            throw new ParamValidationException(paramName, "不能为空");
        }
        
        int length = value.length();
        if (length < min || length > max) {
            throw new ParamValidationException(paramName, 
                    String.format("长度必须在%d-%d之间，当前长度: %d", min, max, length));
        }
    }
    
    /**
     * 校验数值范围
     */
    public static void range(int value, int min, int max, String paramName) {
        if (value < min || value > max) {
            throw new ParamValidationException(paramName, 
                    String.format("必须在%d-%d之间，当前值: %d", min, max, value));
        }
    }
    
    /**
     * 校验版本号格式是否正确
     */
    public static void validVersionFormat(String version, String paramName) {
        notEmpty(version, paramName);
        if (!VERSION_PATTERN.matcher(version).matches()) {
            throw new ParamValidationException(paramName, 
                    "版本号格式不正确，应为 X.Y.Z 格式，例如 1.0.0");
        }
    }
} 