package org.xhy.infrastructure.verification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xhy.infrastructure.verification.storage.CodeStorage;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class VerificationCodeService {
    // 存储邮箱和发送次数的映射，用于防刷
    private final Map<String, LimitInfo> limitMap = new ConcurrentHashMap<>();
    // 存储IP和发送次数的映射，用于防刷
    private final Map<String, IpLimitInfo> ipLimitMap = new ConcurrentHashMap<>();
    
    // 验证码存储接口，可以是内存存储或Redis存储
    private final CodeStorage codeStorage;
    
    // 验证码长度
    private static final int CODE_LENGTH = 6;
    // 验证码有效期（分钟）
    private static final int EXPIRATION_MINUTES = 10;
    // 单个邮箱每日最大发送次数
    private static final int MAX_DAILY_SEND_COUNT = 10;
    // 验证码发送最小间隔（秒）
    private static final int MIN_SEND_INTERVAL_SECONDS = 60;
    // IP每日最大发送次数
    private static final int MAX_DAILY_IP_SEND_COUNT = 20;

    @Autowired
    public VerificationCodeService(CodeStorage codeStorage) {
        this.codeStorage = codeStorage;
    }

    /**
     * 生成邮箱验证码
     * @param email 邮箱
     * @param captchaUuid 图形验证码UUID
     * @param captchaCode 用户输入的图形验证码
     * @param ip 用户IP
     * @return 生成的验证码
     */
    public String generateCode(String email, String captchaUuid, String captchaCode, String ip) {
        // 验证图形验证码
        if (!CaptchaUtils.verifyCaptcha(captchaUuid, captchaCode)) {
            throw new RuntimeException("图形验证码错误或已过期");
        }
        
        // 检查IP限制
        checkIpLimit(ip);
        
        // 检查邮箱发送限制
        checkSendLimit(email);
        
        // 生成6位数字验证码
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        
        // 计算过期时间
        long expirationMillis = TimeUnit.MINUTES.toMillis(EXPIRATION_MINUTES);
        codeStorage.storeCode(email, code.toString(), expirationMillis);
        
        // 记录发送次数
        LimitInfo limitInfo = limitMap.getOrDefault(email, new LimitInfo());
        limitInfo.incrementCount();
        limitInfo.setLastSendTime(System.currentTimeMillis());
        limitMap.put(email, limitInfo);
        
        // 记录IP发送次数
        IpLimitInfo ipLimitInfo = ipLimitMap.getOrDefault(ip, new IpLimitInfo());
        ipLimitInfo.incrementCount();
        ipLimitMap.put(ip, ipLimitInfo);
        
        return code.toString();
    }
    
    /**
     * 验证验证码
     * @param email 邮箱
     * @param code 验证码
     * @return 验证结果
     */
    public boolean verifyCode(String email, String code) {
        return codeStorage.verifyCode(email, code);
    }
    
    private void checkSendLimit(String email) {
        LimitInfo limitInfo = limitMap.get(email);
        if (limitInfo != null) {
            // 检查发送间隔
            long elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - limitInfo.getLastSendTime());
            if (elapsedSeconds < MIN_SEND_INTERVAL_SECONDS) {
                throw new RuntimeException("发送过于频繁，请" + (MIN_SEND_INTERVAL_SECONDS - elapsedSeconds) + "秒后再试");
            }
            
            // 检查日发送次数
            if (limitInfo.getDailyCount() >= MAX_DAILY_SEND_COUNT) {
                throw new RuntimeException("今日发送次数已达上限，请明天再试");
            }
        }
    }
    
    private void checkIpLimit(String ip) {
        IpLimitInfo ipLimitInfo = ipLimitMap.get(ip);
        if (ipLimitInfo != null && ipLimitInfo.getDailyCount() >= MAX_DAILY_IP_SEND_COUNT) {
            throw new RuntimeException("您的IP今日请求次数已达上限，请明天再试");
        }
    }
    
    // 用于定时重置每日发送次数（可在应用启动时设置每日零点执行）
    public void resetAllCounts() {
        limitMap.clear();
        ipLimitMap.clear();
    }
    
    // 限制信息内部类
    private static class LimitInfo {
        private int dailyCount = 0;
        private long lastSendTime = 0;
        
        public void incrementCount() {
            dailyCount++;
        }
        
        public int getDailyCount() {
            return dailyCount;
        }
        
        public long getLastSendTime() {
            return lastSendTime;
        }
        
        public void setLastSendTime(long lastSendTime) {
            this.lastSendTime = lastSendTime;
        }
    }
    
    // IP限制信息内部类
    private static class IpLimitInfo {
        private int dailyCount = 0;
        
        public void incrementCount() {
            dailyCount++;
        }
        
        public int getDailyCount() {
            return dailyCount;
        }
    }
} 