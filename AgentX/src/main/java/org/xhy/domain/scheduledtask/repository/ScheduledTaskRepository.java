package org.xhy.domain.scheduledtask.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.scheduledtask.model.ScheduledTaskEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** 定时任务仓储接口 */
@Mapper
public interface ScheduledTaskRepository extends MyBatisPlusExtRepository<ScheduledTaskEntity> {
}