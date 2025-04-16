package org.xhy.domain.task.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.task.model.TaskEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

import java.util.List;

/**
 * 任务仓储接口
 */
@Mapper
public interface TaskRepository extends MyBatisPlusExtRepository<TaskEntity> {
    

} 