package org.xhy.infrastructure.verification.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的验证码存储实现
 */
public class MemoryCodeStorage implements CodeStorage {
    // 存储验证码的Map，键为存储键，值为验证码信息
    private final Map<String, CodeInfo> codeMap = new ConcurrentHashMap<>();
    
    @Override
    public void storeCode(String key, String code, long expirationMillis) {
        long expirationTime = System.currentTimeMillis() + expirationMillis;
        codeMap.put(key, new CodeInfo(code, expirationTime));
    }
    
    @Override
    public String getCode(String key) {
        CodeInfo codeInfo = codeMap.get(key);
        if (codeInfo == null) {
            return null;
        }
        
        // 检查是否过期
        if (System.currentTimeMillis() > codeInfo.getExpirationTime()) {
            codeMap.remove(key);
            return null;
        }
        
        return codeInfo.getCode();
    }
    
    @Override
    public boolean verifyCode(String key, String code) {
        String storedCode = getCode(key);
        if (storedCode == null) {
            return false;
        }
        
        boolean result = storedCode.equals(code);
        if (result) {
            // 验证成功后移除
            removeCode(key);
        }
        
        return result;
    }
    
    @Override
    public void removeCode(String key) {
        codeMap.remove(key);
    }
    
    @Override
    public void cleanExpiredCodes() {
        long currentTime = System.currentTimeMillis();
        codeMap.entrySet().removeIf(entry -> 
            entry.getValue().getExpirationTime() < currentTime);
    }
    
    // 验证码信息内部类
    private static class CodeInfo {
        private final String code;
        private final long expirationTime;
        
        public CodeInfo(String code, long expirationTime) {
            this.code = code;
            this.expirationTime = expirationTime;
        }
        
        public String getCode() {
            return code;
        }
        
        public long getExpirationTime() {
            return expirationTime;
        }
    }
} 