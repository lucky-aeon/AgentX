package org.xhy.application.scheduledtask.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.scheduledtask.assembler.ScheduledTaskAssembler;
import org.xhy.application.scheduledtask.dto.ScheduledTaskDTO;
import org.xhy.domain.scheduledtask.model.ScheduledTaskEntity;
import org.xhy.domain.scheduledtask.service.ScheduledTaskDomainService;
import org.xhy.domain.scheduledtask.service.ScheduledTaskExecutionService;
import org.xhy.domain.scheduledtask.service.TaskScheduleService;
import org.xhy.interfaces.dto.scheduledtask.request.CreateScheduledTaskRequest;
import org.xhy.interfaces.dto.scheduledtask.request.UpdateScheduledTaskRequest;

import java.time.LocalDateTime;
import java.util.List;

/** 定时任务应用服务 职责： 1. 接收和验证来自接口层的请求 2. 将请求转换为领域对象或参数 3. 调用领域服务执行业务逻辑 4. 转换和返回结果给接口层 */
@Service
public class ScheduledTaskAppService {

    private final ScheduledTaskDomainService scheduledTaskDomainService;
    private final TaskScheduleService taskScheduleService;
    private final ScheduledTaskExecutionService executionService;

    public ScheduledTaskAppService(ScheduledTaskDomainService scheduledTaskDomainService,
            TaskScheduleService taskScheduleService, ScheduledTaskExecutionService executionService) {
        this.scheduledTaskDomainService = scheduledTaskDomainService;
        this.taskScheduleService = taskScheduleService;
        this.executionService = executionService;
    }

    /** 创建定时任务
     * @param request 创建请求
     * @param userId 用户ID
     * @return 创建的任务DTO */
    @Transactional
    public ScheduledTaskDTO createScheduledTask(CreateScheduledTaskRequest request, String userId) {
        // 使用组装器创建领域实体
        ScheduledTaskEntity entity = ScheduledTaskAssembler.toEntity(request, userId);

        // 计算下次执行时间
        LocalDateTime nextExecuteTime = taskScheduleService.calculateNextExecuteTime(entity, LocalDateTime.now());
        entity.setNextExecuteTime(nextExecuteTime);

        // 调用领域服务创建任务
        ScheduledTaskEntity savedEntity = scheduledTaskDomainService.createTask(entity);

        // 调度任务执行
        executionService.scheduleTask(savedEntity);

        return ScheduledTaskAssembler.toDTO(savedEntity);
    }

    /** 更新定时任务
     * @param request 更新请求
     * @param userId 用户ID
     * @return 更新后的任务DTO */
    @Transactional
    public ScheduledTaskDTO updateScheduledTask(UpdateScheduledTaskRequest request, String userId) {
        // 使用组装器创建更新实体
        ScheduledTaskEntity updateEntity = ScheduledTaskAssembler.toEntity(request, userId);

        ScheduledTaskEntity task = scheduledTaskDomainService.getTask(request.getId(), userId);
        updateEntity.setStatus(task.getStatus());
        updateEntity.setSessionId(task.getSessionId());
        updateEntity.setAgentId(task.getAgentId());
        updateEntity.setLastExecuteTime(task.getLastExecuteTime());

        // 如果更新了重复配置，重新计算下次执行时间
        if (updateEntity.getRepeatConfig() != null) {
            LocalDateTime nextExecuteTime = taskScheduleService.calculateNextExecuteTime(updateEntity,
                    LocalDateTime.now());
            updateEntity.setNextExecuteTime(nextExecuteTime);
        }

        // 调用领域服务更新任务
        scheduledTaskDomainService.updateTask(updateEntity);

        // 重新调度任务
        executionService.rescheduleTask(updateEntity);

        return ScheduledTaskAssembler.toDTO(updateEntity);
    }

    /** 删除定时任务
     * @param taskId 任务ID
     * @param userId 用户ID */
    @Transactional
    public void deleteTask(String taskId, String userId) {
        // 通过执行服务删除（会自动取消调度）
        executionService.deleteTask(taskId, userId);
    }

    /** 获取单个定时任务
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 任务DTO */
    public ScheduledTaskDTO getTask(String taskId, String userId) {
        ScheduledTaskEntity entity = scheduledTaskDomainService.getTask(taskId, userId);
        return ScheduledTaskAssembler.toDTO(entity);
    }

    /** 获取用户的定时任务列表
     * @param userId 用户ID
     * @return 任务列表 */
    public List<ScheduledTaskDTO> getUserTasks(String userId) {
        List<ScheduledTaskEntity> entities = scheduledTaskDomainService.getTasksByUserId(userId);
        return entities.stream().map(ScheduledTaskAssembler::toDTO).collect(java.util.stream.Collectors.toList());
    }

    /** 根据会话ID获取定时任务列表
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 任务列表 */
    public List<ScheduledTaskDTO> getTasksBySessionId(String sessionId, String userId) {
        List<ScheduledTaskEntity> entities = scheduledTaskDomainService.getTasksBySessionId(sessionId);
        // 过滤出属于当前用户的任务
        return entities.stream().filter(entity -> userId.equals(entity.getUserId())).map(ScheduledTaskAssembler::toDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    /** 根据Agent ID获取定时任务列表
     * @param agentId Agent ID
     * @param userId 用户ID
     * @return 任务列表 */
    public List<ScheduledTaskDTO> getTasksByAgentId(String agentId, String userId) {
        List<ScheduledTaskEntity> entities = scheduledTaskDomainService.getTasksByAgentId(agentId);
        // 过滤出属于当前用户的任务
        return entities.stream().filter(entity -> userId.equals(entity.getUserId())).map(ScheduledTaskAssembler::toDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    /** 暂停定时任务
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 更新后的任务DTO */
    @Transactional
    public ScheduledTaskDTO pauseTask(String taskId, String userId) {
        executionService.pauseTask(taskId, userId);
        ScheduledTaskEntity entity = scheduledTaskDomainService.getTask(taskId, userId);
        return ScheduledTaskAssembler.toDTO(entity);
    }

    /** 恢复定时任务
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 更新后的任务DTO */
    @Transactional
    public ScheduledTaskDTO resumeTask(String taskId, String userId) {
        executionService.resumeTask(taskId, userId);
        ScheduledTaskEntity entity = scheduledTaskDomainService.getTask(taskId, userId);
        return ScheduledTaskAssembler.toDTO(entity);
    }
}