package org.xhy.domain.user.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.user.model.UserEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/**
 * 模型仓储接口
 */
@Mapper
public interface UserRepository extends MyBatisPlusExtRepository<UserEntity> {
    
   
} 