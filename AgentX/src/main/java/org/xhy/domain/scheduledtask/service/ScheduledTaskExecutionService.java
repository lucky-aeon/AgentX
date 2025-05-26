package org.xhy.domain.scheduledtask.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.scheduledtask.constant.ScheduleTaskStatus;
import org.xhy.domain.scheduledtask.model.ScheduledTaskEntity;

import java.time.LocalDateTime;
import java.util.List;

/** 定时任务执行服务 协调整个定时任务执行流程 */
@Service
public class ScheduledTaskExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskExecutionService.class);

    private final ScheduledTaskDomainService scheduledTaskDomainService;
    private final TaskScheduleService taskScheduleService;
    private final DelayedTaskQueueManager queueManager;

    public ScheduledTaskExecutionService(ScheduledTaskDomainService scheduledTaskDomainService,
            TaskScheduleService taskScheduleService, DelayedTaskQueueManager queueManager) {
        this.scheduledTaskDomainService = scheduledTaskDomainService;
        this.taskScheduleService = taskScheduleService;
        this.queueManager = queueManager;
    }

    /** 初始化服务，加载现有的活跃任务到延迟队列 */
    @PostConstruct
    public void init() {
        loadActiveTasksToQueue();
        logger.info("定时任务执行服务已启动，活跃任务已加载到延迟队列");
    }

    /** 调度新创建的任务
     * @param task 新创建的任务 */
    public void scheduleTask(ScheduledTaskEntity task) {
        if (task == null || !task.isActive()) {
            return;
        }

        LocalDateTime nextExecuteTime = task.getNextExecuteTime();
        if (nextExecuteTime == null) {
            // 计算下次执行时间
            nextExecuteTime = taskScheduleService.calculateNextExecuteTime(task, LocalDateTime.now());
            if (nextExecuteTime != null) {
                task.setNextExecuteTime(nextExecuteTime);
                scheduledTaskDomainService.updateTask(task);
            }
        }

        if (nextExecuteTime != null && nextExecuteTime.isAfter(LocalDateTime.now())) {
            queueManager.addTask(task, nextExecuteTime);
            logger.info("任务已调度: taskId={}, nextExecuteTime={}", task.getId(), nextExecuteTime);
        }
    }

    /** 取消任务调度
     * @param taskId 任务ID */
    public void cancelTask(String taskId) {
        queueManager.removeTask(taskId);
        logger.info("任务调度已取消: taskId={}", taskId);
    }

    public void cancelTasks(List<String> taskIds) {
        taskIds.forEach(this::cancelTask);
    }

    /** 重新调度任务（用于任务更新后）
     * @param task 更新后的任务 */
    public void rescheduleTask(ScheduledTaskEntity task) {
        // 先取消原有调度
        cancelTask(task.getId());

        // 重新调度
        scheduleTask(task);

        logger.info("任务已重新调度: taskId={}", task.getId());
    }

    /** 暂停任务
     * @param taskId 任务ID
     * @param userId 用户ID */
    public void pauseTask(String taskId, String userId) {
        scheduledTaskDomainService.pauseTask(taskId, userId);
        cancelTask(taskId);
        logger.info("任务已暂停: taskId={}, userId={}", taskId, userId);
    }

    /** 恢复任务
     * @param taskId 任务ID
     * @param userId 用户ID */
    public void resumeTask(String taskId, String userId) {
        scheduledTaskDomainService.resumeTask(taskId, userId);

        // 重新加载任务并调度
        ScheduledTaskEntity task = scheduledTaskDomainService.getTask(taskId, userId);
        scheduleTask(task);

        logger.info("任务已恢复: taskId={}, userId={}", taskId, userId);
    }

    /** 删除任务
     * @param taskId 任务ID
     * @param userId 用户ID */
    public void deleteTask(String taskId, String userId) {
        cancelTask(taskId);
        scheduledTaskDomainService.deleteTask(taskId, userId);
        logger.info("任务已删除: taskId={}, userId={}", taskId, userId);
    }

    /** 批量删除指定会话的所有定时任务
     * @param sessionId 会话ID
     * @param userId 用户ID */
    public void deleteTasksBySessionId(String sessionId, String userId) {
        // 先获取要删除的任务列表，用于取消队列中的调度
        List<ScheduledTaskEntity> tasksToDelete = scheduledTaskDomainService.getTasksBySessionId(sessionId).stream()
                .filter(task -> userId.equals(task.getUserId())).toList();

        // 取消延迟队列中的所有相关任务
        tasksToDelete.forEach(task -> cancelTask(task.getId()));

        // 批量删除数据库记录
        int deletedCount = scheduledTaskDomainService.deleteTasksBySessionId(sessionId, userId);

        logger.info("已删除会话 {} 的 {} 个定时任务, userId={}", sessionId, deletedCount, userId);
    }

    /** 批量删除指定Agent的所有定时任务
     * @param agentId Agent ID
     * @param userId 用户ID */
    public void deleteTasksByAgentId(String agentId, String userId) {
        // 先获取要删除的任务列表，用于取消队列中的调度
        List<ScheduledTaskEntity> tasksToDelete = scheduledTaskDomainService.getTasksByAgentId(agentId).stream()
                .filter(task -> userId.equals(task.getUserId())).toList();

        // 取消延迟队列中的所有相关任务
        tasksToDelete.forEach(task -> cancelTask(task.getId()));

        // 批量删除数据库记录
        int deletedCount = scheduledTaskDomainService.deleteTasksByAgentId(agentId, userId);

        logger.info("已删除Agent {} 的 {} 个定时任务, userId={}", agentId, deletedCount, userId);
    }

    /** 获取队列状态信息
     * @return 队列大小 */
    public int getQueueSize() {
        return queueManager.getQueueSize();
    }

    /** 加载现有的活跃任务到延迟队列 */
    private void loadActiveTasksToQueue() {
        try {
            List<ScheduledTaskEntity> activeTasks = scheduledTaskDomainService.getActiveTasksToExecute();
            LocalDateTime now = LocalDateTime.now();

            for (ScheduledTaskEntity task : activeTasks) {
                LocalDateTime nextExecuteTime = task.getNextExecuteTime();

                // 如果没有下次执行时间，计算一个
                if (nextExecuteTime == null) {
                    nextExecuteTime = taskScheduleService.calculateNextExecuteTime(task, now);
                    if (nextExecuteTime != null) {
                        task.setNextExecuteTime(nextExecuteTime);
                        scheduledTaskDomainService.updateTask(task);
                    }
                }

                // 只调度未来的任务
                if (nextExecuteTime != null && nextExecuteTime.isAfter(now)) {
                    queueManager.addTask(task, nextExecuteTime);
                } else if (nextExecuteTime != null && nextExecuteTime.isBefore(now)) {
                    // 过期任务，立即执行一次
                    queueManager.addTask(task, now.plusSeconds(1));
                }
            }

            logger.info("已加载 {} 个活跃任务到延迟队列", activeTasks.size());

        } catch (Exception e) {
            logger.error("加载活跃任务到延迟队列失败: {}", e.getMessage(), e);
        }
    }
}