package org.xhy.domain.scheduledtask.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.xhy.domain.scheduledtask.constant.ScheduleTaskStatus;
import org.xhy.domain.scheduledtask.model.ScheduledTaskEntity;
import org.xhy.domain.scheduledtask.repository.ScheduledTaskRepository;
import org.xhy.infrastructure.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务领域服务
 * 处理定时任务相关的业务逻辑
 */
@Service
public class ScheduledTaskDomainService {

    private final ScheduledTaskRepository scheduledTaskRepository;

    public ScheduledTaskDomainService(ScheduledTaskRepository scheduledTaskRepository) {
        this.scheduledTaskRepository = scheduledTaskRepository;
    }

    /**
     * 创建定时任务
     * @param task 定时任务实体
     * @return 创建后的任务
     */
    public ScheduledTaskEntity createTask(ScheduledTaskEntity task) {
        scheduledTaskRepository.checkInsert(task);
        return task;
    }

    /**
     * 根据用户ID获取定时任务列表
     * @param userId 用户ID
     * @return 定时任务列表
     */
    public List<ScheduledTaskEntity> getTasksByUserId(String userId) {
        return scheduledTaskRepository.selectList(
            Wrappers.<ScheduledTaskEntity>lambdaQuery()
                .eq(ScheduledTaskEntity::getUserId, userId)
                .orderByDesc(ScheduledTaskEntity::getCreatedAt)
        );
    }

    /**
     * 根据用户ID和状态获取定时任务列表
     * @param userId 用户ID
     * @param status 任务状态
     * @return 定时任务列表
     */
    public List<ScheduledTaskEntity> getTasksByUserIdAndStatus(String userId, ScheduleTaskStatus status) {
        return scheduledTaskRepository.selectList(
            Wrappers.<ScheduledTaskEntity>lambdaQuery()
                .eq(ScheduledTaskEntity::getUserId, userId)
                .eq(ScheduledTaskEntity::getStatus, status)
                .orderByDesc(ScheduledTaskEntity::getCreatedAt)
        );
    }

    /**
     * 根据会话ID获取定时任务列表
     * @param sessionId 会话ID
     * @return 定时任务列表
     */
    public List<ScheduledTaskEntity> getTasksBySessionId(String sessionId) {
        return scheduledTaskRepository.selectList(
            Wrappers.<ScheduledTaskEntity>lambdaQuery()
                .eq(ScheduledTaskEntity::getSessionId, sessionId)
                .orderByDesc(ScheduledTaskEntity::getCreatedAt)
        );
    }

    /**
     * 根据Agent ID获取定时任务列表
     * @param agentId Agent ID
     * @return 定时任务列表
     */
    public List<ScheduledTaskEntity> getTasksByAgentId(String agentId) {
        return scheduledTaskRepository.selectList(
            Wrappers.<ScheduledTaskEntity>lambdaQuery()
                .eq(ScheduledTaskEntity::getAgentId, agentId)
                .orderByDesc(ScheduledTaskEntity::getCreatedAt)
        );
    }

    /**
     * 获取需要执行的活跃任务
     * @return 需要执行的任务列表
     */
    public List<ScheduledTaskEntity> getActiveTasksToExecute() {
        return scheduledTaskRepository.selectList(
            Wrappers.<ScheduledTaskEntity>lambdaQuery()
                .eq(ScheduledTaskEntity::getStatus, ScheduleTaskStatus.ACTIVE)
                .orderByAsc(ScheduledTaskEntity::getCreatedAt)
        );
    }

    /**
     * 更新定时任务
     * @param task 更新的任务信息
     */
    public void updateTask(ScheduledTaskEntity task) {
        scheduledTaskRepository.checkedUpdate(task,
            Wrappers.<ScheduledTaskEntity>lambdaUpdate()
                .eq(ScheduledTaskEntity::getId, task.getId())
                .eq(ScheduledTaskEntity::getUserId, task.getUserId())
        );
    }

    /**
     * 删除定时任务
     * @param taskId 任务ID
     * @param userId 用户ID
     */
    public void deleteTask(String taskId, String userId) {
        scheduledTaskRepository.checkedDelete(
            Wrappers.<ScheduledTaskEntity>lambdaQuery()
                .eq(ScheduledTaskEntity::getId, taskId)
                .eq(ScheduledTaskEntity::getUserId, userId)
        );
    }

    /**
     * 暂停定时任务
     * @param taskId 任务ID
     * @param userId 用户ID
     */
    public void pauseTask(String taskId, String userId) {
        ScheduledTaskEntity task = new ScheduledTaskEntity();
        task.setStatus(ScheduleTaskStatus.PAUSED);
        scheduledTaskRepository.checkedUpdate(task,
            Wrappers.<ScheduledTaskEntity>lambdaUpdate()
                .eq(ScheduledTaskEntity::getId, taskId)
                .eq(ScheduledTaskEntity::getUserId, userId)
        );
    }

    /**
     * 恢复定时任务
     * @param taskId 任务ID
     * @param userId 用户ID
     */
    public void resumeTask(String taskId, String userId) {
        ScheduledTaskEntity task = new ScheduledTaskEntity();
        task.setStatus(ScheduleTaskStatus.ACTIVE);
        scheduledTaskRepository.checkedUpdate(task,
            Wrappers.<ScheduledTaskEntity>lambdaUpdate()
                .eq(ScheduledTaskEntity::getId, taskId)
                .eq(ScheduledTaskEntity::getUserId, userId)
        );
    }

    /**
     * 完成定时任务
     * @param taskId 任务ID
     * @param userId 用户ID
     */
    public void completeTask(String taskId, String userId) {
        ScheduledTaskEntity task = new ScheduledTaskEntity();
        task.setStatus(ScheduleTaskStatus.COMPLETED);
        scheduledTaskRepository.checkedUpdate(task,
            Wrappers.<ScheduledTaskEntity>lambdaUpdate()
                .eq(ScheduledTaskEntity::getId, taskId)
                .eq(ScheduledTaskEntity::getUserId, userId)
        );
    }

    /**
     * 记录任务执行时间
     * @param taskId 任务ID
     * @param executeTime 执行时间
     */
    public void recordExecution(String taskId, LocalDateTime executeTime) {
        ScheduledTaskEntity scheduledTaskEntity = new ScheduledTaskEntity();
        scheduledTaskEntity.setNextExecuteTime(executeTime);
        scheduledTaskEntity.setId(taskId);

        scheduledTaskRepository.checkedUpdate(scheduledTaskEntity,
            Wrappers.<ScheduledTaskEntity>lambdaUpdate()
                .eq(ScheduledTaskEntity::getId, taskId)
                    .set(ScheduledTaskEntity::getLastExecuteTime, executeTime)
        );
    }

    /**
     * 检查任务是否存在
     * @param taskId 任务ID
     * @param userId 用户ID
     */
    public void checkTaskExist(String taskId, String userId) {
        ScheduledTaskEntity task = scheduledTaskRepository.selectOne(
            Wrappers.<ScheduledTaskEntity>lambdaQuery()
                .eq(ScheduledTaskEntity::getId, taskId)
                .eq(ScheduledTaskEntity::getUserId, userId)
        );
        if (task == null) {
            throw new BusinessException("定时任务不存在");
        }
    }

    /**
     * 获取定时任务
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 定时任务实体
     */
    public ScheduledTaskEntity getTask(String taskId, String userId) {
        ScheduledTaskEntity task = scheduledTaskRepository.selectOne(
            Wrappers.<ScheduledTaskEntity>lambdaQuery()
                .eq(ScheduledTaskEntity::getId, taskId)
                .eq(ScheduledTaskEntity::getUserId, userId)
        );
        if (task == null) {
            throw new BusinessException("定时任务不存在");
        }
        return task;
    }

    /**
     * 统计用户的定时任务数量
     * @param userId 用户ID
     * @return 任务数量
     */
    public long countByUserId(String userId) {
        return scheduledTaskRepository.selectCount(
            Wrappers.<ScheduledTaskEntity>lambdaQuery()
                .eq(ScheduledTaskEntity::getUserId, userId)
        );
    }

    /**
     * 统计用户指定状态的定时任务数量
     * @param userId 用户ID
     * @param status 任务状态
     * @return 任务数量
     */
    public long countByUserIdAndStatus(String userId, ScheduleTaskStatus status) {
        return scheduledTaskRepository.selectCount(
            Wrappers.<ScheduledTaskEntity>lambdaQuery()
                .eq(ScheduledTaskEntity::getUserId, userId)
                .eq(ScheduledTaskEntity::getStatus, status)
        );
    }

    /**
     * 批量删除指定会话的所有定时任务
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 删除的任务数量
     */
    public int deleteTasksBySessionId(String sessionId, String userId) {
        return scheduledTaskRepository.delete(
            Wrappers.<ScheduledTaskEntity>lambdaQuery()
                .eq(ScheduledTaskEntity::getSessionId, sessionId)
                .eq(ScheduledTaskEntity::getUserId, userId)
        );
    }

    /**
     * 批量删除指定Agent的所有定时任务
     * @param agentId Agent ID
     * @param userId 用户ID
     * @return 删除的任务数量
     */
    public int deleteTasksByAgentId(String agentId, String userId) {
        return scheduledTaskRepository.delete(
            Wrappers.<ScheduledTaskEntity>lambdaQuery()
                .eq(ScheduledTaskEntity::getAgentId, agentId)
                .eq(ScheduledTaskEntity::getUserId, userId)
        );
    }
}