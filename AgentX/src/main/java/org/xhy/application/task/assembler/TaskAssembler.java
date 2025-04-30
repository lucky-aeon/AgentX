package org.xhy.application.task.assembler;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.xhy.application.task.dto.TaskDTO;
import org.xhy.domain.task.model.TaskEntity;

/** 任务对象转换器 */
public class TaskAssembler {

    /** 将实体转换为DTO
     *
     * @param entity 任务实体
     * @return 任务DTO */
    public static TaskDTO toDTO(TaskEntity entity) {
        if (entity == null) {
            return null;
        }

        TaskDTO dto = new TaskDTO();
        dto.setId(entity.getId());
        dto.setSessionId(entity.getSessionId());
        dto.setUserId(entity.getUserId());
        dto.setParentTaskId(entity.getParentTaskId());
        dto.setTaskName(entity.getTaskName());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus().name());
        dto.setProgress(entity.getProgress());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }

    /** 将实体列表转换为DTO列表
     *
     * @param entities 实体列表
     * @return DTO列表 */
    public static List<TaskDTO> toDTOList(List<TaskEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream().map(TaskAssembler::toDTO).collect(Collectors.toList());
    }
}
