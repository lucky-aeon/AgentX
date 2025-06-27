package org.xhy.interfaces.api.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.container.dto.ContainerDTO;
import org.xhy.application.container.service.ContainerAppService;
import org.xhy.domain.container.model.ContainerEntity;
import org.xhy.domain.container.service.ContainerDomainService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.infrastructure.entity.Operator;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.container.request.CreateContainerRequest;
import org.xhy.interfaces.dto.container.request.QueryContainerRequest;
import org.xhy.interfaces.dto.container.response.ContainerStatisticsResponse;

/** 管理员容器管理接口 */
@RestController
@RequestMapping("/admin/containers")
public class AdminContainerController {

    private final ContainerAppService containerAppService;

    public AdminContainerController(ContainerAppService containerAppService) {
        this.containerAppService = containerAppService;
    }

    /** 分页获取容器列表
     * 
     * @param queryRequest 查询参数
     * @return 容器分页列表 */
    @GetMapping
    public Result<Page<ContainerDTO>> getContainers(QueryContainerRequest queryRequest) {
        Page<ContainerEntity> page = new Page<>(queryRequest.getPage(), queryRequest.getPageSize());
        Page<ContainerDTO> result = containerAppService.getContainersPage(
                page, queryRequest.getKeyword(), queryRequest.getStatus(), queryRequest.getType());
        return Result.success(result);
    }

    /** 获取容器统计信息
     * 
     * @return 容器统计数据 */
    @GetMapping("/statistics")
    public Result<ContainerStatisticsResponse> getContainerStatistics() {
        ContainerDomainService.ContainerStatistics statistics = containerAppService.getStatistics();
        ContainerStatisticsResponse response = new ContainerStatisticsResponse();
        response.setTotalContainers(statistics.getTotalContainers());
        response.setRunningContainers(statistics.getRunningContainers());
        return Result.success(response);
    }

    /** 创建审核容器
     * 
     * @param request 容器创建请求
     * @return 创建结果 */
    @PostMapping("/review")
    public Result<ContainerDTO> createReviewContainer(@RequestBody @Validated CreateContainerRequest request) {
        String userId = UserContext.getCurrentUserId();
        ContainerDTO container = containerAppService.createReviewContainer(request, userId);
        return Result.success(container);
    }

    /** 启动容器
     * 
     * @param containerId 容器ID
     * @return 操作结果 */
    @PostMapping("/{containerId}/start")
    public Result<Void> startContainer(@PathVariable String containerId) {
        containerAppService.startContainer(containerId, Operator.ADMIN);
        return Result.success();
    }

    /** 停止容器
     * 
     * @param containerId 容器ID
     * @return 操作结果 */
    @PostMapping("/{containerId}/stop")
    public Result<Void> stopContainer(@PathVariable String containerId) {
        containerAppService.stopContainer(containerId, Operator.ADMIN);
        return Result.success();
    }

    /** 删除容器
     * 
     * @param containerId 容器ID
     * @return 操作结果 */
    @DeleteMapping("/{containerId}")
    public Result<Void> deleteContainer(@PathVariable String containerId) {
        containerAppService.deleteContainer(containerId, Operator.ADMIN);
        return Result.success();
    }

}