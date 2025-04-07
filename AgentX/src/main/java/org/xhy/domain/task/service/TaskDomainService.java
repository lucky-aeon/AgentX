package org.xhy.domain.task.service;

import org.springframework.stereotype.Service;
import org.xhy.domain.task.constant.TaskStatus;
import org.xhy.domain.task.model.TaskEntity;
import org.xhy.domain.task.repository.TaskRepository;
import org.xhy.infrastructure.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务领域服务
 */
@Service
public class TaskDomainService {
    
    private final TaskRepository taskRepository;
    
    public TaskDomainService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }


    public TaskEntity addTask(TaskEntity taskEntity) {
        taskEntity.setStartTime(LocalDateTime.now());
        taskRepository.checkInsert(taskEntity);
        return taskEntity;
    }
    
    /**
     * 更新任务
     *
     * @param taskEntity 任务实体
     * @return 更新后的任务实体
     */
    public TaskEntity updateTask(TaskEntity taskEntity) {
        taskRepository.checkedUpdateById(taskEntity);
        return taskEntity;
    }
}