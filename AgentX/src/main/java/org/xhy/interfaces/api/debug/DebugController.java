package org.xhy.interfaces.api.debug;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.domain.llm.model.config.ProviderConfig;
import org.xhy.domain.llm.repository.ProviderRepository;
import org.xhy.infrastructure.utils.DbCheckUtil;
import org.xhy.infrastructure.utils.EncryptUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 调试专用控制器
 * 仅在开发环境使用
 */
@RestController
@RequestMapping("/debug")
public class DebugController {
    
    private static final Logger log = LoggerFactory.getLogger(DebugController.class);
    
    private final DbCheckUtil dbCheckUtil;
    private final SqlSessionFactory sqlSessionFactory;
    private final ProviderRepository providerRepository;
    
    public DebugController(
            DbCheckUtil dbCheckUtil, 
            SqlSessionFactory sqlSessionFactory,
            ProviderRepository providerRepository) {
        this.dbCheckUtil = dbCheckUtil;
        this.sqlSessionFactory = sqlSessionFactory;
        this.providerRepository = providerRepository;
    }
    
    /**
     * 检查Provider的配置信息
     */
    @GetMapping("/provider/{id}/check")
    public Map<String, Object> checkProviderConfig(@PathVariable("id") String providerId) {
        dbCheckUtil.checkProviderConfig(providerId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "检查完成，请查看日志");
        
        // 尝试通过仓库获取
        ProviderEntity provider = providerRepository.selectById(providerId);
        if (provider != null) {
            result.put("provider_id", provider.getId());
            result.put("provider_name", provider.getName());
            
            // 获取加密配置
            ProviderConfig encryptedConfig = provider.getEncryptedConfig();
            if (encryptedConfig != null) {
                result.put("encrypted_config_api_key", encryptedConfig.getApiKey());
                result.put("encrypted_config_base_url", encryptedConfig.getBaseUrl());
            } else {
                result.put("encrypted_config", "null");
            }
            
            // 获取解密配置
            ProviderConfig decryptedConfig = provider.getConfig();
            if (decryptedConfig != null) {
                result.put("decrypted_config_api_key", decryptedConfig.getApiKey());
                result.put("decrypted_config_base_url", decryptedConfig.getBaseUrl());
            } else {
                result.put("decrypted_config", "null");
            }
        } else {
            result.put("provider", "未找到");
        }
        
        return result;
    }
    
    /**
     * 修复Provider的配置信息
     */
    @PostMapping("/provider/{id}/fix")
    public String fixProviderConfig(@PathVariable("id") String providerId) {
        dbCheckUtil.fixProviderConfig(providerId);
        return "修复完成，请查看日志";
    }
    
    /**
     * 检查MyBatis类型处理器注册情况
     */
    @GetMapping("/typehandlers")
    public Map<String, Object> checkTypeHandlers() {
        TypeHandlerRegistry registry = sqlSessionFactory.getConfiguration().getTypeHandlerRegistry();
        
        Map<String, Object> result = new HashMap<>();
        result.put("typeHandlerCount", registry.getTypeHandlers().size());
        
        // 检查是否有注册我们的类型处理器
        boolean hasProviderConfigHandler = false;
        for (Object handler : registry.getTypeHandlers()) {
            if (handler.getClass().getSimpleName().contains("ProviderConfig")) {
                hasProviderConfigHandler = true;
                result.put("providerConfigHandler", handler.getClass().getName());
            }
        }
        result.put("hasProviderConfigHandler", hasProviderConfigHandler);
        
        return result;
    }
    
    /**
     * 测试加密解密功能
     */
    @GetMapping("/crypto")
    public Map<String, String> testCrypto(@RequestParam String text) {
        Map<String, String> result = new HashMap<>();
        
        result.put("original", text);
        
        try {
            String encrypted = EncryptUtils.encrypt(text);
            result.put("encrypted", encrypted);
            
            String decrypted = EncryptUtils.decrypt(encrypted);
            result.put("decrypted", decrypted);
            
            result.put("success", String.valueOf(text.equals(decrypted)));
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        return result;
    }
} 