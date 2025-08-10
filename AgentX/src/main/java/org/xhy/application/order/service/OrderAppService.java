package org.xhy.application.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xhy.application.order.assembler.OrderAssembler;
import org.xhy.application.order.dto.OrderDTO;
import org.xhy.application.order.dto.QueryAllOrderRequest;
import org.xhy.application.order.dto.QueryUserOrderRequest;
import org.xhy.domain.order.constant.OrderStatus;
import org.xhy.domain.order.constant.OrderType;
import org.xhy.domain.order.model.OrderEntity;
import org.xhy.domain.order.service.OrderDomainService;
import org.xhy.domain.user.model.UserEntity;
import org.xhy.domain.user.service.UserDomainService;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 订单应用服务 */
@Service
public class OrderAppService {

    private static final Logger logger = LoggerFactory.getLogger(OrderAppService.class);

    private final OrderDomainService orderDomainService;
    private final UserDomainService userDomainService;

    public OrderAppService(OrderDomainService orderDomainService, UserDomainService userDomainService) {
        this.orderDomainService = orderDomainService;
        this.userDomainService = userDomainService;
    }

    /** 获取用户已支付订单列表（分页）
     * 
     * @param queryRequest 查询条件
     * @param userId 用户ID
     * @return 订单分页数据 */
    public Page<OrderDTO> getUserPaidOrders(QueryUserOrderRequest queryRequest, String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException("用户ID不能为空");
        }

        logger.info("获取用户已支付订单列表: userId={}, page={}, pageSize={}", userId, queryRequest.getPage(),
                queryRequest.getPageSize());

        // 转换查询条件
        OrderType orderType = null;
        if (StringUtils.hasText(queryRequest.getOrderType())) {
            orderType = OrderType.fromCode(queryRequest.getOrderType());
        }

        // 用户只能查看已支付的订单
        OrderStatus status = OrderStatus.PAID;

        // 调用领域服务进行分页查询
        Page<OrderEntity> entityPage = orderDomainService.getOrdersByUserIdWithPage(userId, queryRequest.getPage(),
                queryRequest.getPageSize(), orderType, status);

        // 转换为DTO
        List<OrderDTO> orderDTOList = OrderAssembler.toDTOs(entityPage.getRecords());

        // 创建返回的分页对象
        Page<OrderDTO> resultPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        resultPage.setRecords(orderDTOList);

        logger.info("获取用户已支付订单成功: userId={}, total={}", userId, entityPage.getTotal());
        return resultPage;
    }

    /** 获取用户订单详情
     * 
     * @param orderId 订单ID
     * @param userId 用户ID
     * @return 订单详情 */
    public OrderDTO getUserOrderDetail(String orderId, String userId) {
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException("订单ID不能为空");
        }
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException("用户ID不能为空");
        }

        logger.info("获取用户订单详情: orderId={}, userId={}", orderId, userId);

        OrderEntity order = orderDomainService.getOrderById(orderId);

        // 验证订单归属和状态
        if (!userId.equals(order.getUserId())) {
            throw new BusinessException("无权限访问此订单");
        }

        // 用户只能查看已支付的订单
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException("订单状态不允许查看");
        }

        OrderDTO result = OrderAssembler.toDTO(order);
        logger.info("获取用户订单详情成功: orderId={}", orderId);
        return result;
    }

    /** 管理员获取所有订单列表（分页+搜索）
     * 
     * @param queryRequest 查询条件
     * @return 订单分页数据 */
    public Page<OrderDTO> getAllOrders(QueryAllOrderRequest queryRequest) {
        logger.info("管理员获取所有订单列表: page={}, pageSize={}, keyword={}", queryRequest.getPage(), queryRequest.getPageSize(),
                queryRequest.getKeyword());

        // 转换查询条件
        OrderType orderType = null;
        if (StringUtils.hasText(queryRequest.getOrderType())) {
            orderType = OrderType.fromCode(queryRequest.getOrderType());
        }

        OrderStatus status = null;
        if (queryRequest.getStatus() != null) {
            status = OrderStatus.fromCode(queryRequest.getStatus());
        }

        // 调用领域服务进行分页查询
        Page<OrderEntity> entityPage = orderDomainService.getAllOrdersWithPage(queryRequest.getPage(),
                queryRequest.getPageSize(), queryRequest.getUserId(), orderType, status, queryRequest.getKeyword());

        // 转换为DTO
        List<OrderDTO> orderDTOList = OrderAssembler.toDTOs(entityPage.getRecords());

        // 管理员查询需要补充用户昵称信息
        if (!orderDTOList.isEmpty()) {
            // 获取所有用户ID
            List<String> userIds = orderDTOList.stream().map(OrderDTO::getUserId).distinct()
                    .collect(Collectors.toList());

            // 批量查询用户信息
            Map<String, String> userNicknameMap = userIds.stream()
                    .collect(Collectors.toMap(userId -> userId, userId -> {
                        UserEntity user = userDomainService.getUserInfo(userId);
                        return user != null ? user.getNickname() : "未知用户";
                    }));

            // 为每个订单设置用户昵称
            orderDTOList.forEach(order -> order.setUserNickname(userNicknameMap.get(order.getUserId())));
        }

        // 创建返回的分页对象
        Page<OrderDTO> resultPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        resultPage.setRecords(orderDTOList);

        logger.info("管理员获取所有订单成功: total={}", entityPage.getTotal());
        return resultPage;
    }

    /** 管理员获取订单详情
     * 
     * @param orderId 订单ID
     * @return 订单详情 */
    public OrderDTO getOrderDetail(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException("订单ID不能为空");
        }

        logger.info("管理员获取订单详情: orderId={}", orderId);

        OrderEntity order = orderDomainService.getOrderById(orderId);
        OrderDTO result = OrderAssembler.toDTO(order);

        logger.info("管理员获取订单详情成功: orderId={}", orderId);
        return result;
    }
}