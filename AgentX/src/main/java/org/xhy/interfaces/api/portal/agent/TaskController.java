package org.xhy.interfaces.api.portal.agent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.task.dto.TaskDTO;
import org.xhy.application.task.service.TaskAppService;
import org.xhy.domain.task.model.TaskAggregate;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;

import java.util.List;

/** agent任务管理 */
@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskAppService taskAppService;

    @Autowired
    public TaskController(TaskAppService taskAppService) {
        this.taskAppService = taskAppService;
    }

    /** 获取当前会话的任务
     * @param sessionId 会话id */
    @GetMapping("/session/{sessionId}/latest")
    public Result<TaskAggregate> getSessionTasks(@PathVariable String sessionId) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(taskAppService.getCurrentSessionTask(sessionId, userId));
    }
}