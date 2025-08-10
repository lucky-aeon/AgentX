package org.xhy.application.order.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.order.dto.OrderDTO;
import org.xhy.domain.order.constant.OrderStatus;
import org.xhy.domain.order.constant.OrderType;
import org.xhy.domain.order.constant.PaymentPlatform;
import org.xhy.domain.order.constant.PaymentType;
import org.xhy.domain.order.model.OrderEntity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** 订单实体与DTO转换器 */
public class OrderAssembler {

    /** 将订单实体转换为DTO
     * 
     * @param entity 订单实体
     * @return 订单DTO */
    public static OrderDTO toDTO(OrderEntity entity) {
        if (entity == null) {
            return null;
        }

        OrderDTO dto = new OrderDTO();
        BeanUtils.copyProperties(entity, dto, "orderType", "paymentPlatform", "paymentType");

        // 订单类型转换
        if (entity.getOrderType() != null) {
            dto.setOrderType(entity.getOrderType().getCode());
        }

        // 订单状态转换
        if (entity.getStatus() != null) {
            dto.setStatus(entity.getStatus().getCode());
            dto.setStatusName(entity.getStatus().getDescription());
        }

        // 支付平台转换
        if (entity.getPaymentPlatform() != null) {
            dto.setPaymentPlatform(entity.getPaymentPlatform().getCode());
            dto.setPaymentPlatformName(entity.getPaymentPlatform().getName());
        }

        // 支付类型转换
        if (entity.getPaymentType() != null) {
            dto.setPaymentType(entity.getPaymentType().getCode());
            dto.setPaymentTypeName(entity.getPaymentType().getName());
        }

        return dto;
    }

    /** 将订单实体列表转换为DTO列表
     * 
     * @param entities 订单实体列表
     * @return 订单DTO列表 */
    public static List<OrderDTO> toDTOs(List<OrderEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(OrderAssembler::toDTO).collect(Collectors.toList());
    }

    /** 从DTO构建订单实体（创建订单时使用）
     * 
     * @param dto 订单DTO
     * @param userId 用户ID
     * @return 订单实体 */
    public static OrderEntity toEntity(OrderDTO dto, String userId) {
        if (dto == null) {
            return null;
        }

        OrderEntity entity = new OrderEntity();
        BeanUtils.copyProperties(dto, entity, "orderType", "status", "paymentPlatform", "paymentType");

        // 设置用户ID
        entity.setUserId(userId);

        // 订单类型转换
        if (dto.getOrderType() != null) {
            entity.setOrderType(OrderType.fromCode(dto.getOrderType()));
        }

        // 订单状态转换
        if (dto.getStatus() != null) {
            entity.setStatus(OrderStatus.fromCode(dto.getStatus()));
        }

        // 支付平台转换
        if (dto.getPaymentPlatform() != null) {
            entity.setPaymentPlatform(PaymentPlatform.fromCode(dto.getPaymentPlatform()));
        }

        // 支付类型转换
        if (dto.getPaymentType() != null) {
            entity.setPaymentType(PaymentType.fromCode(dto.getPaymentType()));
        }

        return entity;
    }
}