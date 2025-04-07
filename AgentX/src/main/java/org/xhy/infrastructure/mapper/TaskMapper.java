package org.xhy.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.task.model.TaskEntity;

/**
 * 任务Mapper接口
 */
@Mapper
public interface TaskMapper extends BaseMapper<TaskEntity> {
} 