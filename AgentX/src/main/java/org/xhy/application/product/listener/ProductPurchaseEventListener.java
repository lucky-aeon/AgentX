package org.xhy.application.product.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.xhy.domain.order.constant.OrderType;
import org.xhy.domain.order.event.PurchaseSuccessEvent;

import java.util.Map;

/** 商品购买事件监听器（示例） 监听购买成功事件，处理商品购买类型的订单
 * 
 * 这是一个扩展示例，展示如何为不同的订单类型添加处理逻辑 */
@Component
public class ProductPurchaseEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ProductPurchaseEventListener.class);

    /** 处理购买成功事件，执行商品发货等逻辑
     * 
     * @param event 购买成功事件 */
    @EventListener
    @Async
    public void handlePurchaseSuccess(PurchaseSuccessEvent event) {
        try {
            // 只处理商品购买订单
            if (event.getOrderType() != OrderType.PURCHASE) {
                logger.debug("跳过非商品购买订单: orderNo={}, orderType={}", event.getOrderNo(), event.getOrderType());
                return;
            }

            logger.info("开始处理商品购买: userId={}, orderNo={}, amount={}", event.getUserId(), event.getOrderNo(),
                    event.getAmount());

            // 这里可以实现商品发货逻辑
            // 例如：
            // 1. 从订单metadata中获取商品信息
            // 2. 调用商品服务进行发货
            // 3. 更新库存
            // 4. 发送发货通知

            Map<String, Object> metadata = event.getOrderEntity().getMetadata();
            if (metadata != null) {
                String productId = (String) metadata.get("productId");
                Integer quantity = (Integer) metadata.get("quantity");

                logger.info("商品购买详情: productId={}, quantity={}", productId, quantity);

                // 示例：调用商品服务
                // productService.deliverProduct(productId, quantity, event.getUserId());
            }

            logger.info("商品购买处理完成: userId={}, orderNo={}", event.getUserId(), event.getOrderNo());

        } catch (Exception e) {
            logger.error("商品购买处理失败: userId={}, orderNo={}", event.getUserId(), event.getOrderNo(), e);

            // 这里可以添加失败处理逻辑，如发送告警、重试等
            // 由于使用了@Async，异常不会影响主流程
        }
    }
}