package org.xhy.domain.task.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.task.model.TaskEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** 任务仓储接口 */
@Mapper
public interface TaskRepository extends MyBatisPlusExtRepository<TaskEntity> {
}
