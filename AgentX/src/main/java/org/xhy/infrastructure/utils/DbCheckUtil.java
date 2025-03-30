package org.xhy.infrastructure.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库检查工具
 * 用于检查数据库中的原始数据格式
 */
@Component
public class DbCheckUtil {
    
    private static final Logger log = LoggerFactory.getLogger(DbCheckUtil.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 检查Provider表中指定ID的config字段内容
     */
    public void checkProviderConfig(String providerId) {
        String sql = "SELECT config FROM providers WHERE id = ?";
        String configValue = jdbcTemplate.queryForObject(sql, String.class, providerId);
        
        log.info("Provider[{}] config原始值: {}", providerId, configValue);
        
        // 尝试判断格式
        if (configValue != null) {
            if (configValue.trim().startsWith("{") && configValue.trim().endsWith("}")) {
                log.info("格式分析: 看起来是JSON格式");
            } else if (configValue.contains("==")) {
                log.info("格式分析: 看起来是Base64编码的加密内容");
                try {
                    String decrypted = EncryptUtils.decrypt(configValue);
                    log.info("尝试解密结果: {}", decrypted);
                } catch (Exception e) {
                    log.error("解密失败", e);
                }
            } else {
                log.info("格式分析: 未知格式");
            }
        }
    }
    
    /**
     * 修复Provider表中指定ID的config字段
     * 如果发现是纯JSON，转换为加密格式
     */
    public void fixProviderConfig(String providerId) {
        String sql = "SELECT config FROM providers WHERE id = ?";
        String configValue = jdbcTemplate.queryForObject(sql, String.class, providerId);
        
        if (configValue != null && configValue.trim().startsWith("{") && configValue.trim().endsWith("}")) {
            log.info("检测到JSON格式，进行加密处理: {}", configValue);
            String encrypted = EncryptUtils.encrypt(configValue);
            log.info("加密后: {}", encrypted);
            
            // 更新数据库
            String updateSql = "UPDATE providers SET config = ? WHERE id = ?";
            int rows = jdbcTemplate.update(updateSql, encrypted, providerId);
            log.info("更新数据库结果: 影响行数 {}", rows);
        } else {
            log.info("不需要修复，保持原值");
        }
    }
} 