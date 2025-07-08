package org.xhy.interfaces.api.portal.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xhy.application.auth.dto.AuthConfigDTO;
import org.xhy.application.auth.service.AuthSettingAppService;
import org.xhy.interfaces.api.common.Result;

/** 认证配置控制器（用户端） */
@RestController
@RequestMapping("/auth")
public class AuthConfigController {

    private final AuthSettingAppService authSettingAppService;

    public AuthConfigController(AuthSettingAppService authSettingAppService) {
        this.authSettingAppService = authSettingAppService;
    }

    /** 获取可用的认证配置
     * 
     * @return 认证配置 */
    @GetMapping("/config")
    public Result<AuthConfigDTO> getAuthConfig() {
        AuthConfigDTO config = authSettingAppService.getAuthConfig();
        return Result.success(config);
    }
}