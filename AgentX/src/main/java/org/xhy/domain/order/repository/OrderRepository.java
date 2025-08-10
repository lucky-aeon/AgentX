package org.xhy.domain.order.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.order.model.OrderEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** 订单仓储接口 */
@Mapper
public interface OrderRepository extends MyBatisPlusExtRepository<OrderEntity> {
}