package org.xhy.interfaces.api.portal;

import org.springframework.web.bind.annotation.*;
import org.xhy.application.container.dto.ContainerDTO;
import org.xhy.application.container.service.ContainerAppService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.container.response.ContainerHealthStatusResponse;

/** 用户容器管理接口 */
@RestController
@RequestMapping("/api/containers")
public class ContainerController {

    private final ContainerAppService containerAppService;

    public ContainerController(ContainerAppService containerAppService) {
        this.containerAppService = containerAppService;
    }

    /** 获取当前用户的容器
     * 
     * @return 用户容器信息 */
    @GetMapping("/user")
    public Result<ContainerDTO> getUserContainer() {
        String userId = UserContext.getCurrentUserId();
        ContainerDTO container = containerAppService.getUserContainer(userId);
        return Result.success(container);
    }

    /** 为当前用户创建容器
     * 
     * @return 创建的容器信息 */
    @PostMapping("/user")
    public Result<ContainerDTO> createUserContainer() {
        String userId = UserContext.getCurrentUserId();
        ContainerDTO container = containerAppService.createUserContainer(userId);
        return Result.success(container);
    }

    /** 检查用户容器健康状态
     * 
     * @return 健康状态检查结果 */
    @GetMapping("/user/health")
    public Result<ContainerHealthStatusResponse> checkUserContainerHealth() {
        String userId = UserContext.getCurrentUserId();
        ContainerAppService.ContainerHealthStatus healthStatus = 
                containerAppService.checkUserContainerHealth(userId);
        
        ContainerHealthStatusResponse response = new ContainerHealthStatusResponse();
        response.setHealthy(healthStatus.isHealthy());
        response.setMessage(healthStatus.getMessage());
        response.setContainer(healthStatus.getContainer());
        
        return Result.success(response);
    }
}