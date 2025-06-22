package org.xhy.interfaces.api.admin.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xhy.application.user.dto.UserDTO;
import org.xhy.application.user.service.UserAppService;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.user.request.QueryUserRequest;

/**
 * 管理员用户管理接口
 */
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserAppService userAppService;

    public AdminUserController(UserAppService userAppService) {
        this.userAppService = userAppService;
    }

    /**
     * 分页获取用户列表
     * 
     * @param queryUserRequest 查询参数
     * @return 用户分页列表
     */
    @GetMapping
    public Result<Page<UserDTO>> getUsers(QueryUserRequest queryUserRequest) {
        return Result.success(userAppService.getUsers(queryUserRequest));
    }
}