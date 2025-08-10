package org.xhy.domain.product.repository;

import org.apache.ibatis.annotations.Mapper;
import org.xhy.domain.product.model.ProductEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/** 商品仓储接口 */
@Mapper
public interface ProductRepository extends MyBatisPlusExtRepository<ProductEntity> {
}