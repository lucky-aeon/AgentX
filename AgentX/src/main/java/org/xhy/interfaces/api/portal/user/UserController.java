package org.xhy.interfaces.api.portal.user;


import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.user.dto.UserDTO;
import org.xhy.application.user.service.LoginAppService;
import org.xhy.application.user.service.UserAppService;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.user.request.LoginRequest;
import org.xhy.interfaces.dto.user.request.RegisterRequest;

import java.util.Map;

/**
 * 用户
 */
@RestController
@RequestMapping("/users")
public class UserController {

   private final UserAppService userAppService;


    public UserController(UserAppService userAppService) {
        this.userAppService = userAppService;
    }

    /**
     * 获取用户信息
     * @param userId
     * @return
     */
    @GetMapping("/{userId}")
    public Result<UserDTO> getUserInfo(@PathVariable String userId){
        return Result.success(userAppService.getUserInfo(userId));
    }
}
