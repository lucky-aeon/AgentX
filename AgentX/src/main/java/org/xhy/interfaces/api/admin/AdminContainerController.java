package org.xhy.interfaces.api.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.container.dto.ContainerDTO;
import org.xhy.application.container.service.ContainerAppService;
import org.xhy.application.container.service.ReviewContainerService;
import org.xhy.domain.container.model.ContainerEntity;
import org.xhy.domain.container.service.ContainerDomainService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.container.request.CreateContainerRequest;
import org.xhy.interfaces.dto.container.request.QueryContainerRequest;
import org.xhy.interfaces.dto.container.response.ContainerStatisticsResponse;

/** 管理员容器管理接口 */
@RestController
@RequestMapping("/admin/containers")
public class AdminContainerController {

    private final ContainerAppService containerAppService;
    private final ReviewContainerService reviewContainerService;

    public AdminContainerController(ContainerAppService containerAppService,
            ReviewContainerService reviewContainerService) {
        this.containerAppService = containerAppService;
        this.reviewContainerService = reviewContainerService;
    }

    /** 分页获取容器列表
     * 
     * @param queryRequest 查询参数
     * @return 容器分页列表 */
    @GetMapping
    public Result<Page<ContainerDTO>> getContainers(QueryContainerRequest queryRequest) {
        Page<ContainerEntity> page = new Page<>(queryRequest.getPage(), queryRequest.getPageSize());
        Page<ContainerDTO> result = containerAppService.getContainersPage(page, queryRequest.getKeyword(),
                queryRequest.getStatus(), queryRequest.getType());
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

    /** 删除容器
     * 
     * @param containerId 容器ID
     * @return 操作结果 */
    @DeleteMapping("/{containerId}")
    public Result<Void> deleteContainer(@PathVariable String containerId) {
        containerAppService.deleteContainer(containerId);
        return Result.success();
    }

    /** 获取容器日志
     * 
     * @param containerId 容器ID
     * @param lines 获取日志行数（可选，默认100行）
     * @return 容器日志 */
    @GetMapping("/{containerId}/logs")
    public Result<String> getContainerLogs(@PathVariable String containerId,
            @RequestParam(required = false) Integer lines) {
        String logs = containerAppService.getContainerLogs(containerId, lines);
        return Result.success(logs);
    }

    /** 获取容器系统信息
     * 
     * @param containerId 容器ID
     * @return 系统信息 */
    @GetMapping("/{containerId}/system-info")
    public Result<String> getSystemInfo(@PathVariable String containerId) {
        String info = containerAppService.getContainerSystemInfo(containerId);
        return Result.success(info);
    }

    /** 获取容器进程信息
     * 
     * @param containerId 容器ID
     * @return 进程信息 */
    @GetMapping("/{containerId}/processes")
    public Result<String> getProcessInfo(@PathVariable String containerId) {
        String info = containerAppService.getContainerProcessInfo(containerId);
        return Result.success(info);
    }

    /** 获取容器网络信息
     * 
     * @param containerId 容器ID
     * @return 网络信息 */
    @GetMapping("/{containerId}/network")
    public Result<String> getNetworkInfo(@PathVariable String containerId) {
        String info = containerAppService.getContainerNetworkInfo(containerId);
        return Result.success(info);
    }

    /** 检查容器内MCP网关状态
     * 
     * @param containerId 容器ID
     * @return MCP网关状态 */
    @GetMapping("/{containerId}/mcp-status")
    public Result<String> getMcpGatewayStatus(@PathVariable String containerId) {
        String status = containerAppService.checkMcpGatewayStatus(containerId);
        return Result.success(status);
    }

    /** 启动容器
     * 
     * @param containerId 容器ID
     * @return 操作结果 */
    @PostMapping("/{containerId}/start")
    public Result<Void> startContainer(@PathVariable String containerId) {
        containerAppService.startContainer(containerId);
        return Result.success();
    }

    /** 停止容器
     * 
     * @param containerId 容器ID
     * @return 操作结果 */
    @PostMapping("/{containerId}/stop")
    public Result<Void> stopContainer(@PathVariable String containerId) {
        containerAppService.stopContainer(containerId);
        return Result.success();
    }

    /** 获取或创建审核容器
     * 
     * @return 审核容器信息 */
    @PostMapping("/review")
    public Result<ContainerDTO> getOrCreateReviewContainer() {
        ContainerDTO reviewContainer = containerAppService.getOrCreateReviewContainer();
        return Result.success(reviewContainer);
    }

    /** 检查审核容器健康状态
     * 
     * @return 审核容器健康状态 */
    @GetMapping("/review/health")
    public Result<ReviewContainerService.ReviewContainerHealthStatus> checkReviewContainerHealth() {
        ReviewContainerService.ReviewContainerHealthStatus healthStatus = reviewContainerService
                .checkReviewContainerHealth();
        return Result.success(healthStatus);
    }

    /** 重新创建审核容器
     * 
     * @return 新的审核容器信息 */
    @PostMapping("/review/recreate")
    public Result<ContainerDTO> recreateReviewContainer() {
        ContainerDTO reviewContainer = reviewContainerService.recreateReviewContainer();
        return Result.success(reviewContainer);
    }

    /** 从模板创建容器
     * 
     * @param templateId 模板ID
     * @return 创建的容器信息 */
    @PostMapping("/from-template/{templateId}")
    public Result<ContainerDTO> createContainerFromTemplate(@PathVariable String templateId) {
        ContainerDTO container = containerAppService.createContainerFromTemplate(templateId);
        return Result.success(container);
    }

}