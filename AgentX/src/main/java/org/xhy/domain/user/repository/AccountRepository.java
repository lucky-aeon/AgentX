package org.xhy.domain.user.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.user.model.AccountEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** 账户仓储接口 */
@Mapper
public interface AccountRepository extends MyBatisPlusExtRepository<AccountEntity> {
}