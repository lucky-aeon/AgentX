package org.xhy.interfaces.api.portal.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.xhy.application.user.service.SsoAppService;
import org.xhy.interfaces.api.common.Result;

import java.util.Map;

@RestController
@RequestMapping("/sso")
public class SsoController {

    private final SsoAppService ssoAppService;

    public SsoController(SsoAppService ssoAppService) {
        this.ssoAppService = ssoAppService;
    }

    /** 获取SSO登录URL
     * @param provider SSO提供商（community、github等）
     * @param redirectUrl 登录成功后的回调地址
     * @return 登录URL */
    @GetMapping("/{provider}/login")
    public Result<Map<String, String>> getSsoLoginUrl(
            @PathVariable String provider,
            @RequestParam(required = false) String redirectUrl) {
        
        String loginUrl = ssoAppService.getSsoLoginUrl(provider, redirectUrl);
        return Result.success(Map.of("loginUrl", loginUrl));
    }

    /** SSO登录回调处理
     * @param provider SSO提供商
     * @param code 授权码
     * @return 登录token */
    @GetMapping("/{provider}/callback")
    public Result<Map<String, Object>> handleSsoCallback(
            @PathVariable String provider,
            @RequestParam String code) {
        
        String token = ssoAppService.handleSsoCallback(provider, code);
        return Result.success("登录成功", Map.of("token", token));
    }
}