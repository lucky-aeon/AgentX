package org.xhy.application.conversation.service.message.agent.manager;

import org.springframework.stereotype.Component;
import org.xhy.application.conversation.service.handler.content.ChatContext;
import org.xhy.domain.task.constant.TaskStatus;
import org.xhy.domain.task.model.TaskEntity;
import org.xhy.domain.task.service.TaskDomainService;

/**
 * 任务管理器
 * 封装任务实体的创建和状态更新
 */
@Component
public class TaskManager {
    private final TaskDomainService taskDomainService;
    
    public TaskManager(TaskDomainService taskDomainService) {
        this.taskDomainService = taskDomainService;
    }
    
    /**
     * 创建父任务
     * 
     * @param context 聊天上下文
     * @return 创建的父任务实体
     */
    public TaskEntity createParentTask(ChatContext context) {
        TaskEntity task = new TaskEntity();
        task.setTaskName(context.getUserMessage());
        task.setSessionId(context.getSessionId());
        task.setUserId(context.getUserId());
        task.setParentTaskId("0");
        task.setProgress(0);
        task.setStatus(TaskStatus.IN_PROGRESS);
        taskDomainService.addTask(task);
        return task;
    }
    
    /**
     * 创建子任务
     * 
     * @param taskName 任务名称
     * @param parentTaskId 父任务ID
     * @param context 聊天上下文
     * @return 创建的子任务实体
     */
    public TaskEntity createSubTask(String taskName, String parentTaskId, ChatContext context) {
        TaskEntity task = new TaskEntity();
        task.setTaskName(taskName);
        task.setSessionId(context.getSessionId());
        task.setUserId(context.getUserId());
        task.setParentTaskId(parentTaskId);
        task.updateStatus(TaskStatus.WAITING);
        taskDomainService.addTask(task);
        return task;
    }
    
    /**
     * 更新任务状态
     * 
     * @param task 任务实体
     * @param status 新状态
     */
    public void updateTaskStatus(TaskEntity task, TaskStatus status) {
        task.updateStatus(status);
        taskDomainService.updateTask(task);
    }
    
    /**
     * 更新任务进度
     * 
     * @param task 任务实体
     * @param completed 已完成数量
     * @param total 总数量
     */
    public void updateTaskProgress(TaskEntity task, int completed, int total) {
        int progress = (int)((completed / (double)total) * 100);
        task.updateProgress(progress);
        taskDomainService.updateTask(task);
    }
    
    /**
     * 完成任务
     * 
     * @param task 任务实体
     * @param result 任务结果
     */
    public void completeTask(TaskEntity task, String result) {
        task.updateStatus(TaskStatus.COMPLETED);
        task.setTaskResult(result);
        taskDomainService.updateTask(task);
    }
} 