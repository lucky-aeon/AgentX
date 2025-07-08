package org.xhy.infrastructure.verification.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/** 基于内存的验证码存储实现 */
public class MemoryCodeStorage implements CodeStorage {
    private static final Logger logger = Logger.getLogger(MemoryCodeStorage.class.getName());
    // 存储验证码的Map，键为存储键，值为验证码信息
    private final Map<String, CodeInfo> codeMap = new ConcurrentHashMap<>();
    // 定时任务执行器
    private final ScheduledExecutorService scheduler;
    // 清理任务执行间隔（分钟）
    private static final int CLEANUP_INTERVAL_MINUTES = 5;

    public MemoryCodeStorage() {
        // 初始化定时任务
        scheduler = Executors.newSingleThreadScheduledExecutor();
        // 定期执行清理任务，每5分钟清理一次过期验证码
        scheduler.scheduleAtFixedRate(this::cleanExpiredCodes, CLEANUP_INTERVAL_MINUTES, CLEANUP_INTERVAL_MINUTES,
                TimeUnit.MINUTES);
        logger.info("验证码过期清理任务已启动，每" + CLEANUP_INTERVAL_MINUTES + "分钟执行一次");
    }

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
        int count = 0;
        for (Map.Entry<String, CodeInfo> entry : codeMap.entrySet()) {
            if (entry.getValue().getExpirationTime() < currentTime) {
                codeMap.remove(entry.getKey());
                count++;
            }
        }
        if (count > 0) {
            logger.info("已清理 " + count + " 个过期验证码");
        }
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