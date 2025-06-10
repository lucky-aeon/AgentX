package org.xhy.interfaces.api.portal.scheduledtask;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.scheduledtask.dto.ScheduledTaskDTO;
import org.xhy.application.scheduledtask.service.ScheduledTaskAppService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.scheduledtask.request.CreateScheduledTaskRequest;
import org.xhy.interfaces.dto.scheduledtask.request.UpdateScheduledTaskRequest;

import java.util.List;

/** 定时任务管理控制器 */
@RestController
@RequestMapping("/scheduled-tasks")
public class PortalScheduledTaskController {

    private final ScheduledTaskAppService scheduledTaskAppService;

    public PortalScheduledTaskController(ScheduledTaskAppService scheduledTaskAppService) {
        this.scheduledTaskAppService = scheduledTaskAppService;
    }

    /** 创建定时任务 */
    @PostMapping
    public Result<ScheduledTaskDTO> createScheduledTask(@RequestBody @Validated CreateScheduledTaskRequest request) {
        String userId = UserContext.getCurrentUserId();
        ScheduledTaskDTO task = scheduledTaskAppService.createScheduledTask(request, userId);
        return Result.success(task);
    }

    /** 更新定时任务 */
    @PutMapping("/{taskId}")
    public Result<ScheduledTaskDTO> updateScheduledTask(@PathVariable String taskId,
            @RequestBody @Validated UpdateScheduledTaskRequest request) {
        String userId = UserContext.getCurrentUserId();
        request.setId(taskId);
        ScheduledTaskDTO task = scheduledTaskAppService.updateScheduledTask(request, userId);
        return Result.success(task);
    }

    /** 删除定时任务 */
    @DeleteMapping("/{taskId}")
    public Result<Void> deleteScheduledTask(@PathVariable String taskId) {
        String userId = UserContext.getCurrentUserId();
        scheduledTaskAppService.deleteTask(taskId, userId);
        return Result.success();
    }

    /** 获取用户的定时任务列表 */
    @GetMapping
    public Result<List<ScheduledTaskDTO>> getScheduledTasks(@RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String agentId) {
        String userId = UserContext.getCurrentUserId();
        List<ScheduledTaskDTO> tasks;

        if (agentId != null && !agentId.trim().isEmpty()) {
            tasks = scheduledTaskAppService.getTasksByAgentId(agentId, userId);
        } else if (sessionId != null && !sessionId.trim().isEmpty()) {
            tasks = scheduledTaskAppService.getTasksBySessionId(sessionId, userId);
        } else {
            tasks = scheduledTaskAppService.getUserTasks(userId);
        }

        return Result.success(tasks);
    }

    /** 根据Agent ID获取定时任务列表 */
    @GetMapping("/agent/{agentId}")
    public Result<List<ScheduledTaskDTO>> getScheduledTasksByAgent(@PathVariable String agentId) {
        String userId = UserContext.getCurrentUserId();
        List<ScheduledTaskDTO> tasks = scheduledTaskAppService.getTasksByAgentId(agentId, userId);
        return Result.success(tasks);
    }

    /** 获取单个定时任务详情 */
    @GetMapping("/{taskId}")
    public Result<ScheduledTaskDTO> getScheduledTask(@PathVariable String taskId) {
        String userId = UserContext.getCurrentUserId();
        ScheduledTaskDTO task = scheduledTaskAppService.getTask(taskId, userId);
        return Result.success(task);
    }

    /** 暂停定时任务
     * @param taskId 任务ID
     * @return 更新后的任务信息 */
    @PostMapping("/{taskId}/pause")
    public Result<ScheduledTaskDTO> pauseTask(@PathVariable String taskId) {
        String userId = UserContext.getCurrentUserId();
        ScheduledTaskDTO task = scheduledTaskAppService.pauseTask(taskId, userId);
        return Result.success(task);
    }

    /** 恢复定时任务
     * @param taskId 任务ID
     * @return 更新后的任务信息 */
    @PostMapping("/{taskId}/resume")
    public Result<ScheduledTaskDTO> resumeTask(@PathVariable String taskId) {
        String userId = UserContext.getCurrentUserId();
        ScheduledTaskDTO task = scheduledTaskAppService.resumeTask(taskId, userId);
        return Result.success(task);
    }

}