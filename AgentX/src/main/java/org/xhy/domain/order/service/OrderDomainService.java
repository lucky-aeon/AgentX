package org.xhy.domain.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xhy.domain.order.constant.OrderStatus;
import org.xhy.domain.order.constant.OrderType;
import org.xhy.domain.order.model.OrderEntity;
import org.xhy.domain.order.repository.OrderRepository;
import org.xhy.infrastructure.entity.Operator;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.exception.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 订单领域服务 */
@Service
public class OrderDomainService {

    private final OrderRepository orderRepository;

    public OrderDomainService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /** 创建订单
     * 
     * @param order 订单实体
     * @return 订单实体 */
    public OrderEntity createOrder(OrderEntity order) {
        order.validate();
        orderRepository.insert(order);
        return order;
    }

    /** 根据ID获取订单
     * 
     * @param orderId 订单ID
     * @return 订单实体
     * @throws EntityNotFoundException 订单不存在时抛出异常 */
    public OrderEntity getOrderById(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException("订单ID不能为空");
        }

        OrderEntity order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new EntityNotFoundException("订单不存在: " + orderId);
        }
        return order;
    }

    /** 根据订单号获取订单
     * 
     * @param orderNo 订单号
     * @return 订单实体，不存在返回null */
    public OrderEntity findOrderByOrderNo(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return null;
        }

        LambdaQueryWrapper<OrderEntity> wrapper = Wrappers.<OrderEntity>lambdaQuery()
                .eq(OrderEntity::getOrderNo, orderNo).last("LIMIT 1");
        return orderRepository.selectOne(wrapper);
    }

    /** 根据订单号获取订单
     * 
     * @param orderNo 订单号
     * @return 订单实体
     * @throws EntityNotFoundException 订单不存在时抛出异常 */
    public OrderEntity getOrderByOrderNo(String orderNo) {
        OrderEntity order = findOrderByOrderNo(orderNo);
        if (order == null) {
            throw new EntityNotFoundException("订单不存在: " + orderNo);
        }
        return order;
    }

    /** 根据用户ID获取订单列表
     * 
     * @param userId 用户ID
     * @param orderType 订单类型（可选）
     * @param status 订单状态（可选）
     * @return 订单列表 */
    public List<OrderEntity> getOrdersByUserId(String userId, OrderType orderType, OrderStatus status) {
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException("用户ID不能为空");
        }

        LambdaQueryWrapper<OrderEntity> wrapper = Wrappers.<OrderEntity>lambdaQuery().eq(OrderEntity::getUserId, userId)
                .eq(orderType != null, OrderEntity::getOrderType, orderType)
                .eq(status != null, OrderEntity::getStatus, status).orderByDesc(OrderEntity::getCreatedAt);

        return orderRepository.selectList(wrapper);
    }

    /** 分页查询用户订单
     * 
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @param orderType 订单类型（可选）
     * @param status 订单状态（可选）
     * @return 分页结果 */
    public Page<OrderEntity> getOrdersByUserIdWithPage(String userId, int page, int pageSize, OrderType orderType,
            OrderStatus status) {
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException("用户ID不能为空");
        }

        LambdaQueryWrapper<OrderEntity> wrapper = Wrappers.<OrderEntity>lambdaQuery().eq(OrderEntity::getUserId, userId)
                .eq(orderType != null, OrderEntity::getOrderType, orderType)
                .eq(status != null, OrderEntity::getStatus, status).orderByDesc(OrderEntity::getCreatedAt);

        Page<OrderEntity> orderPage = new Page<>(page, pageSize);
        return orderRepository.selectPage(orderPage, wrapper);
    }

    /** 管理员分页查询所有订单
     * 
     * @param page 页码
     * @param pageSize 每页大小
     * @param userId 用户ID（可选）
     * @param orderType 订单类型（可选）
     * @param status 订单状态（可选）
     * @param keyword 关键词搜索（可选）
     * @return 分页结果 */
    public Page<OrderEntity> getAllOrdersWithPage(int page, int pageSize, String userId, OrderType orderType,
            OrderStatus status, String keyword) {
        LambdaQueryWrapper<OrderEntity> wrapper = Wrappers.<OrderEntity>lambdaQuery()
                .eq(StringUtils.hasText(userId), OrderEntity::getUserId, userId)
                .eq(orderType != null, OrderEntity::getOrderType, orderType)
                .eq(status != null, OrderEntity::getStatus, status);

        // 关键词搜索：订单号、标题、描述
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(OrderEntity::getOrderNo, keyword).or().like(OrderEntity::getTitle, keyword).or()
                    .like(OrderEntity::getDescription, keyword));
        }

        wrapper.orderByDesc(OrderEntity::getCreatedAt);

        Page<OrderEntity> orderPage = new Page<>(page, pageSize);
        return orderRepository.selectPage(orderPage, wrapper);
    }

    /** 更新订单
     * 
     * @param order 订单实体
     * @param operator 操作者 */
    public void updateOrder(OrderEntity order, Operator operator) {
        if (order == null || !StringUtils.hasText(order.getId())) {
            throw new BusinessException("订单信息不完整");
        }

        order.validate();

        LambdaUpdateWrapper<OrderEntity> wrapper = Wrappers.<OrderEntity>lambdaUpdate()
                .eq(OrderEntity::getId, order.getId())
                .eq(operator.needCheckUserId(), OrderEntity::getUserId, order.getUserId());

        orderRepository.checkedUpdate(order, wrapper);
    }

    /** 标记订单为已支付
     * 
     * @param orderId 订单ID
     * @param operator 操作者 */
    public void markOrderAsPaid(String orderId, Operator operator) {
        OrderEntity order = getOrderById(orderId);
        order.markAsPaid();
        updateOrder(order, operator);
    }

    /** 取消订单
     * 
     * @param orderId 订单ID
     * @param operator 操作者 */
    public void cancelOrder(String orderId, Operator operator) {
        OrderEntity order = getOrderById(orderId);
        order.cancel();
        updateOrder(order, operator);
    }

    /** 退款订单
     * 
     * @param orderId 订单ID
     * @param refundAmount 退款金额
     * @param operator 操作者 */
    public void refundOrder(String orderId, BigDecimal refundAmount, Operator operator) {
        OrderEntity order = getOrderById(orderId);
        order.refund(refundAmount);
        updateOrder(order, operator);
    }

    /** 查找过期的未支付订单
     * 
     * @param limit 查询数量限制
     * @return 过期订单列表 */
    public List<OrderEntity> findExpiredPendingOrders(int limit) {
        LambdaQueryWrapper<OrderEntity> wrapper = Wrappers.<OrderEntity>lambdaQuery()
                .eq(OrderEntity::getStatus, OrderStatus.PENDING).le(OrderEntity::getExpiredAt, LocalDateTime.now())
                .orderByAsc(OrderEntity::getExpiredAt).last("LIMIT " + limit);

        return orderRepository.selectList(wrapper);
    }

    /** 批量标记订单为已过期
     * 
     * @param orderIds 订单ID列表 */
    public void markOrdersAsExpired(List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return;
        }

        OrderEntity updateEntity = new OrderEntity();
        updateEntity.setStatus(OrderStatus.EXPIRED);

        LambdaUpdateWrapper<OrderEntity> wrapper = Wrappers.<OrderEntity>lambdaUpdate().in(OrderEntity::getId, orderIds)
                .eq(OrderEntity::getStatus, OrderStatus.PENDING);

        orderRepository.update(updateEntity, wrapper);
    }

    /** 统计用户订单数量
     * 
     * @param userId 用户ID
     * @param orderType 订单类型（可选）
     * @param status 订单状态（可选）
     * @return 订单数量 */
    public Long countOrdersByUserId(String userId, OrderType orderType, OrderStatus status) {
        if (!StringUtils.hasText(userId)) {
            return 0L;
        }

        LambdaQueryWrapper<OrderEntity> wrapper = Wrappers.<OrderEntity>lambdaQuery().eq(OrderEntity::getUserId, userId)
                .eq(orderType != null, OrderEntity::getOrderType, orderType)
                .eq(status != null, OrderEntity::getStatus, status);

        return orderRepository.selectCount(wrapper);
    }

    /** 计算用户订单总金额
     * 
     * @param userId 用户ID
     * @param orderType 订单类型（可选）
     * @param status 订单状态（可选）
     * @return 订单总金额 */
    public BigDecimal sumOrderAmountByUserId(String userId, OrderType orderType, OrderStatus status) {
        if (!StringUtils.hasText(userId)) {
            return BigDecimal.ZERO;
        }

        List<OrderEntity> orders = getOrdersByUserId(userId, orderType, status);
        return orders.stream().map(OrderEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 更新订单状态
     * 
     * @param orderId 订单ID
     * @param newStatus 新状态 */
    public void updateOrderStatus(String orderId, OrderStatus newStatus) {
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException("订单ID不能为空");
        }
        if (newStatus == null) {
            throw new BusinessException("订单状态不能为空");
        }

        OrderEntity updateEntity = new OrderEntity();
        updateEntity.setStatus(newStatus);

        LambdaUpdateWrapper<OrderEntity> wrapper = Wrappers.<OrderEntity>lambdaUpdate().eq(OrderEntity::getId, orderId);

        int updated = orderRepository.update(updateEntity, wrapper);
        if (updated == 0) {
            throw new EntityNotFoundException("订单不存在或更新失败: " + orderId);
        }
    }

    /** 更新订单状态和支付平台信息
     * 
     * @param orderId 订单ID
     * @param newStatus 新状态
     * @param providerOrderId 第三方支付平台订单ID
     * @param providerOrderId 第三方支付平台支付ID */
    public void updateOrderStatusAndProviderInfo(String orderId, OrderStatus newStatus, String providerOrderId) {
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException("订单ID不能为空");
        }
        if (newStatus == null) {
            throw new BusinessException("订单状态不能为空");
        }

        OrderEntity updateEntity = new OrderEntity();
        updateEntity.setStatus(newStatus);

        // 只有当支付平台信息不为空时才更新
        if (StringUtils.hasText(providerOrderId)) {
            updateEntity.setProviderOrderId(providerOrderId);
        }

        // 如果状态变为已支付，设置支付时间
        if (newStatus == OrderStatus.PAID) {
            updateEntity.setPaidAt(LocalDateTime.now());
        }

        LambdaUpdateWrapper<OrderEntity> wrapper = Wrappers.<OrderEntity>lambdaUpdate().eq(OrderEntity::getId, orderId);

        int updated = orderRepository.update(updateEntity, wrapper);
        if (updated == 0) {
            throw new EntityNotFoundException("订单不存在或更新失败: " + orderId);
        }
    }
}