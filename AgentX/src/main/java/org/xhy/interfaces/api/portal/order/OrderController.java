package org.xhy.interfaces.api.portal.order;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.order.dto.OrderDTO;
import org.xhy.application.order.dto.QueryUserOrderRequest;
import org.xhy.application.order.service.OrderAppService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;

/** 订单控制器 */
@RestController
@RequestMapping("/orders")
@Validated
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderAppService orderAppService;

    public OrderController(OrderAppService orderAppService) {
        this.orderAppService = orderAppService;
    }

    /** 获取当前用户的已支付订单列表
     * 
     * @param queryRequest 查询参数
     * @return 订单分页列表 */
    @GetMapping
    public Result<Page<OrderDTO>> getUserOrders(QueryUserOrderRequest queryRequest) {
        String userId = UserContext.getCurrentUserId();

        logger.info("获取用户订单列表: userId={}, page={}, pageSize={}", userId, queryRequest.getPage(),
                queryRequest.getPageSize());

        try {
            Page<OrderDTO> orders = orderAppService.getUserPaidOrders(queryRequest, userId);
            logger.info("获取用户订单列表成功: userId={}, total={}", userId, orders.getTotal());
            return Result.success(orders);

        } catch (Exception e) {
            logger.error("获取用户订单列表失败: userId={}", userId, e);
            return Result.error(500, "获取订单列表失败: " + e.getMessage());
        }
    }

    /** 获取订单详情
     * 
     * @param orderId 订单ID
     * @return 订单详情 */
    @GetMapping("/{orderId}")
    public Result<OrderDTO> getOrderDetail(@PathVariable String orderId) {
        String userId = UserContext.getCurrentUserId();

        logger.info("获取订单详情: orderId={}, userId={}", orderId, userId);

        try {
            OrderDTO order = orderAppService.getUserOrderDetail(orderId, userId);
            logger.info("获取订单详情成功: orderId={}", orderId);
            return Result.success(order);

        } catch (Exception e) {
            logger.error("获取订单详情失败: orderId={}, userId={}", orderId, userId, e);
            return Result.error(500, "获取订单详情失败: " + e.getMessage());
        }
    }
}