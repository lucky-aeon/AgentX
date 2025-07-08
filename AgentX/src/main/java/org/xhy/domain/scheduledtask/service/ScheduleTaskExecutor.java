package org.xhy.domain.scheduledtask.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.xhy.domain.scheduledtask.constant.RepeatType;
import org.xhy.domain.scheduledtask.event.ScheduledTaskExecuteEvent;
import org.xhy.domain.scheduledtask.model.ScheduledTaskEntity;

import java.time.LocalDateTime;

/** 定时任务执行器 负责实际执行定时任务，通过事件发布与Application层解耦 */
@Service
public class ScheduleTaskExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleTaskExecutor.class);

    private final ApplicationEventPublisher eventPublisher;
    private final ScheduledTaskDomainService scheduledTaskDomainService;
    private final TaskScheduleService taskScheduleService;

    public ScheduleTaskExecutor(ApplicationEventPublisher eventPublisher,
            ScheduledTaskDomainService scheduledTaskDomainService, TaskScheduleService taskScheduleService) {
        this.eventPublisher = eventPublisher;
        this.scheduledTaskDomainService = scheduledTaskDomainService;
        this.taskScheduleService = taskScheduleService;
    }

    /** 执行定时任务
     * @param task 定时任务实体 */
    public void executeTask(ScheduledTaskEntity task) {
        try {
            logger.info("开始执行定时任务: taskId={}, content={}", task.getId(), task.getContent());

            // 检查任务状态
            if (!task.isActive()) {
                logger.warn("任务状态不是ACTIVE，跳过执行: taskId={}, status={}", task.getId(), task.getStatus());
                return;
            }

            // 发布任务执行事件，由Application层监听并处理实际的对话逻辑
            ScheduledTaskExecuteEvent event = new ScheduledTaskExecuteEvent(this, task.getId(), task.getUserId(),
                    task.getSessionId(), task.getContent());
            eventPublisher.publishEvent(event);

            logger.info("定时任务执行事件已发布: taskId={}", task.getId());

            // 记录执行时间并处理下次执行
            handleTaskExecution(task);

        } catch (Exception e) {
            logger.error("定时任务执行异常: taskId={}, error={}", task.getId(), e.getMessage(), e);
        }
    }

    /** 处理任务执行后的逻辑
     * @param task 任务实体 */
    private void handleTaskExecution(ScheduledTaskEntity task) {
        try {
            LocalDateTime now = LocalDateTime.now();

            // 记录执行时间
            task.recordExecution();
            scheduledTaskDomainService.recordExecution(task.getId(), now);

            // 计算并处理下次执行时间
            LocalDateTime nextExecuteTime = taskScheduleService.calculateNextExecuteTime(task, now);

            if (nextExecuteTime != null) {
                // 更新下次执行时间
                task.setNextExecuteTime(nextExecuteTime);
                scheduledTaskDomainService.updateTask(task);
                logger.info("任务下次执行时间已更新: taskId={}, nextTime={}", task.getId(), nextExecuteTime);
            } else {
                // 任务已完成（一次性任务或重复任务到达截止时间）
                task.complete();
                scheduledTaskDomainService.completeTask(task.getId(), task.getUserId());

                if (task.getRepeatType() == RepeatType.NONE) {
                    logger.info("一次性任务执行完成: taskId={}", task.getId());
                } else {
                    logger.info("重复任务已到截止时间，执行完成: taskId={}", task.getId());
                }
            }

        } catch (Exception e) {
            logger.error("处理任务执行后逻辑失败: taskId={}, error={}", task.getId(), e.getMessage(), e);
        }
    }

    /** 检查任务是否可以执行
     * @param task 任务实体
     * @return 是否可以执行 */
    public boolean canExecute(ScheduledTaskEntity task) {
        if (task == null) {
            return false;
        }

        if (!task.isActive()) {
            logger.debug("任务状态不是ACTIVE: taskId={}, status={}", task.getId(), task.getStatus());
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (!taskScheduleService.shouldExecuteAt(task, now)) {
            logger.debug("任务当前时间不应该执行: taskId={}, currentTime={}", task.getId(), now);
            return false;
        }

        return true;
    }
}