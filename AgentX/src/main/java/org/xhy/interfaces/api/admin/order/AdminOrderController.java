package org.xhy.interfaces.api.admin.order;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.order.dto.OrderDTO;
import org.xhy.application.order.dto.QueryAllOrderRequest;
import org.xhy.application.order.service.OrderAppService;
import org.xhy.interfaces.api.common.Result;

/** 管理员订单管理接口 */
@RestController
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private static final Logger logger = LoggerFactory.getLogger(AdminOrderController.class);

    private final OrderAppService orderAppService;

    public AdminOrderController(OrderAppService orderAppService) {
        this.orderAppService = orderAppService;
    }

    /** 分页获取所有订单列表
     * 
     * @param queryAllOrderRequest 查询参数
     * @return 订单分页列表 */
    @GetMapping
    public Result<Page<OrderDTO>> getAllOrders(QueryAllOrderRequest queryAllOrderRequest) {
        logger.info("管理员获取订单列表: page={}, pageSize={}, keyword={}", queryAllOrderRequest.getPage(),
                queryAllOrderRequest.getPageSize(), queryAllOrderRequest.getKeyword());

        try {
            Page<OrderDTO> orders = orderAppService.getAllOrders(queryAllOrderRequest);
            logger.info("管理员获取订单列表成功: total={}", orders.getTotal());
            return Result.success(orders);

        } catch (Exception e) {
            logger.error("管理员获取订单列表失败", e);
            return Result.error(500, "获取订单列表失败: " + e.getMessage());
        }
    }

    /** 获取订单详情
     * 
     * @param orderId 订单ID
     * @return 订单详情 */
    @GetMapping("/{orderId}")
    public Result<OrderDTO> getOrderDetail(@PathVariable String orderId) {
        logger.info("管理员获取订单详情: orderId={}", orderId);

        try {
            OrderDTO order = orderAppService.getOrderDetail(orderId);
            logger.info("管理员获取订单详情成功: orderId={}", orderId);
            return Result.success(order);

        } catch (Exception e) {
            logger.error("管理员获取订单详情失败: orderId={}", orderId, e);
            return Result.error(500, "获取订单详情失败: " + e.getMessage());
        }
    }
}