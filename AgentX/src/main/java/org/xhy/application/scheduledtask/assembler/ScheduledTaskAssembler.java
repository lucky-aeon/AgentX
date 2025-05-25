package org.xhy.application.scheduledtask.assembler;

import org.xhy.application.scheduledtask.dto.ScheduledTaskDTO;
import org.xhy.domain.scheduledtask.constant.ScheduleTaskStatus;
import org.xhy.domain.scheduledtask.model.ScheduledTaskEntity;
import org.xhy.interfaces.dto.scheduledtask.request.CreateScheduledTaskRequest;
import org.xhy.interfaces.dto.scheduledtask.request.UpdateScheduledTaskRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 定时任务组装器
 * 负责DTO和实体之间的转换
 */
public class ScheduledTaskAssembler {

    /**
     * 将创建请求转换为实体
     * @param request 创建请求
     * @param userId 用户ID
     * @return 实体对象
     */
    public static ScheduledTaskEntity toEntity(CreateScheduledTaskRequest request, String userId) {
        ScheduledTaskEntity entity = new ScheduledTaskEntity();
        entity.setUserId(userId);
        entity.setAgentId(request.getAgentId());
        entity.setSessionId(request.getSessionId());
        entity.setContent(request.getContent());
        entity.setRepeatType(request.getRepeatType());
        entity.setRepeatConfig(request.getRepeatConfig());
        entity.setStatus(ScheduleTaskStatus.ACTIVE); // 默认为激活状态
        return entity;
    }

    /**
     * 将更新请求转换为实体
     * @param request 更新请求
     * @param userId 用户ID
     * @return 实体对象
     */
    public static ScheduledTaskEntity toEntity(UpdateScheduledTaskRequest request, String userId) {
        ScheduledTaskEntity entity = new ScheduledTaskEntity();
        entity.setId(request.getId());
        entity.setUserId(userId);
        entity.setContent(request.getContent());
        entity.setRepeatType(request.getRepeatType());
        entity.setRepeatConfig(request.getRepeatConfig());
        entity.setStatus(request.getStatus());
        return entity;
    }

    /**
     * 将实体转换为DTO
     * @param entity 实体对象
     * @return DTO对象
     */
    public static ScheduledTaskDTO toDTO(ScheduledTaskEntity entity) {
        if (entity == null) {
            return null;
        }

        ScheduledTaskDTO dto = new ScheduledTaskDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setAgentId(entity.getAgentId());
        dto.setSessionId(entity.getSessionId());
        dto.setContent(entity.getContent());
        dto.setRepeatType(entity.getRepeatType());
        dto.setRepeatConfig(entity.getRepeatConfig());
        dto.setStatus(entity.getStatus());
        dto.setLastExecuteTime(entity.getLastExecuteTime());
        dto.setNextExecuteTime(entity.getNextExecuteTime());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    /**
     * 将实体列表转换为DTO列表
     * @param entities 实体列表
     * @return DTO列表
     */
    public static List<ScheduledTaskDTO> toDTOs(List<ScheduledTaskEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(ScheduledTaskAssembler::toDTO)
                .collect(Collectors.toList());
    }
} 