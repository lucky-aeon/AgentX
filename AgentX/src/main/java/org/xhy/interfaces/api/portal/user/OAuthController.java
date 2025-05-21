package org.xhy.interfaces.api.portal.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.xhy.application.user.service.OAuthAppService;
import org.xhy.interfaces.api.common.Result;

import java.util.Map;

/** OAuth认证接口 */
@RestController
@RequestMapping("/oauth")
public class OAuthController {

    private final OAuthAppService oauthAppService;

    public OAuthController(OAuthAppService oauthAppService) {
        this.oauthAppService = oauthAppService;
    }

    /** 获取GitHub授权URL
     * @return 授权URL */
    @GetMapping("/github/authorize")
    public Result<Map<String, String>> authorizeGitHub() {
        String authorizeUrl = oauthAppService.getGitHubAuthorizeUrl();
        return Result.success(Map.of("authorizeUrl", authorizeUrl));
    }

    /** 处理GitHub回调
     * @param code 授权码
     * @return 登录响应 */
    @GetMapping("/github/callback")
    public Result<Map<String, String>> githubCallback(@RequestParam String code) {
        Map<String, String> tokenInfo = oauthAppService.handleGitHubCallback(code);
        return Result.success("GitHub登录成功", tokenInfo);
    }
}