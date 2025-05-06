package org.xhy.interfaces.api.portal.user;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xhy.application.user.service.LoginAppService;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.user.request.LoginRequest;
import org.xhy.interfaces.dto.user.request.RegisterRequest;

import java.util.Map;

@RestController
@RequestMapping
public class LoginController {

    private final LoginAppService loginAppService;

    public LoginController(LoginAppService loginAppService) {
        this.loginAppService = loginAppService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Validated LoginRequest loginRequest) {
        String token = loginAppService.login(loginRequest);
        return Result.success(Map.of("token", token));
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody @Validated RegisterRequest registerRequest) {
        loginAppService.register(registerRequest);
        return Result.success().message("注册成功");
    }
}
