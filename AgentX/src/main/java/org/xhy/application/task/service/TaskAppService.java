package org.xhy.application.task.service;

import org.springframework.stereotype.Service;
import org.xhy.application.task.assembler.TaskAssembler;
import org.xhy.application.task.dto.TaskDTO;
import org.xhy.domain.task.model.TaskEntity;
import org.xhy.domain.task.service.TaskDomainService;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务应用服务
 */
@Service
public class TaskAppService {

    private final TaskDomainService taskDomainService;


    public TaskAppService(TaskDomainService taskDomainService
    ) {
        this.taskDomainService = taskDomainService;
    }


    /**
     * 获取会话相关的所有任务
     *
     * @param sessionId 会话ID
     * @return 任务DTO列表
     */
    public List<TaskDTO> getSessionTasks(String sessionId) {
        // 获取任务列表
        List<TaskEntity> tasks = new ArrayList<>(); // todo xhy

        // 转换为DTO返回
        return TaskAssembler.toDTOList(tasks);
    }

} 