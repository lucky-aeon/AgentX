package org.xhy.interfaces.api.portal.agent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.task.dto.TaskDTO;
import org.xhy.application.task.service.TaskAppService;
import org.xhy.interfaces.api.common.Result;

import java.util.List;

/**
 * 任务控制器 - 仅提供查询API
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskAppService taskAppService;

    @Autowired
    public TaskController(TaskAppService taskAppService) {
        this.taskAppService = taskAppService;
    }

    /**
     * 获取会话相关的所有任务
     *
     * @param sessionId 会话ID
     * @return 任务列表
     */
    @GetMapping("/session/{sessionId}")
    public Result<List<TaskDTO>> getSessionTasks(@PathVariable String sessionId) {
        List<TaskDTO> tasks = taskAppService.getSessionTasks(sessionId);
        return Result.success(tasks);
    }
}