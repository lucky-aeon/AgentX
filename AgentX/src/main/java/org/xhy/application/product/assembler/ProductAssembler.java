package org.xhy.application.product.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.product.dto.ProductDTO;
import org.xhy.domain.product.constant.BillingType;
import org.xhy.domain.product.model.ProductEntity;
import org.xhy.interfaces.dto.product.request.CreateProductRequest;
import org.xhy.interfaces.dto.product.request.UpdateProductRequest;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** 商品装配器 处理Entity、DTO、Request之间的转换 */
public class ProductAssembler {

    /** 将Entity转换为DTO
     * @param entity 商品实体
     * @return 商品DTO */
    public static ProductDTO toDTO(ProductEntity entity) {
        if (entity == null) {
            return null;
        }

        ProductDTO dto = new ProductDTO();
        BeanUtils.copyProperties(entity, dto, "type"); // 排除type字段

        // 手动设置type字段
        if (entity.getType() != null) {
            dto.setType(entity.getType().getCode());
        }

        return dto;
    }

    /** 将Entity列表转换为DTO列表
     * @param entities 商品实体列表
     * @return 商品DTO列表 */
    public static List<ProductDTO> toDTOs(List<ProductEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(ProductAssembler::toDTO).collect(Collectors.toList());
    }

    /** 将创建请求转换为Entity
     * @param request 创建商品请求
     * @return 商品实体 */
    public static ProductEntity toEntity(CreateProductRequest request) {
        if (request == null) {
            return null;
        }

        ProductEntity entity = new ProductEntity();
        BeanUtils.copyProperties(request, entity, "type"); // 排除type字段

        // 手动设置type字段
        if (request.getType() != null) {
            entity.setType(BillingType.fromCode(request.getType()));
        }

        return entity;
    }

    /** 将更新请求转换为Entity
     * @param request 更新商品请求
     * @return 商品实体 */
    public static ProductEntity toEntity(UpdateProductRequest request) {
        if (request == null) {
            return null;
        }

        ProductEntity entity = new ProductEntity();
        BeanUtils.copyProperties(request, entity);
        return entity;
    }

    /** 更新Entity的字段（从更新请求）
     * @param entity 目标实体
     * @param request 更新请求 */
    public static void updateEntity(ProductEntity entity, UpdateProductRequest request) {
        if (entity == null || request == null) {
            return;
        }

        // 只更新非空字段
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getType() != null) {
            entity.setType(BillingType.fromCode(request.getType()));
        }
        if (request.getServiceId() != null) {
            entity.setServiceId(request.getServiceId());
        }
        if (request.getRuleId() != null) {
            entity.setRuleId(request.getRuleId());
        }
        if (request.getPricingConfig() != null) {
            entity.setPricingConfig(request.getPricingConfig());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
    }

    public static ProductEntity toEntity(UpdateProductRequest request, String productId) {
        ProductEntity entity = new ProductEntity();
        BeanUtils.copyProperties(request, entity);
        entity.setId(productId);
        entity.setType(BillingType.fromCode(request.getType()));
        return entity;
    }
}