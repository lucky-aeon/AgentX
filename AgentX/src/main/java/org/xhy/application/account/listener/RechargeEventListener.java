package org.xhy.application.account.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.account.service.AccountAppService;
import org.xhy.domain.order.constant.OrderType;
import org.xhy.domain.order.event.PurchaseSuccessEvent;
import org.xhy.interfaces.dto.account.request.RechargeRequest;

/** 余额充值事件监听器 监听购买成功事件，处理余额充值类型的订单 */
@Component
public class RechargeEventListener {

    private static final Logger logger = LoggerFactory.getLogger(RechargeEventListener.class);

    private final AccountAppService accountAppService;

    public RechargeEventListener(AccountAppService accountAppService) {
        this.accountAppService = accountAppService;
    }

    /** 处理购买成功事件，执行余额充值
     * 
     * @param event 购买成功事件 */
    @EventListener
    @Async
    public void handlePurchaseSuccess(PurchaseSuccessEvent event) {
        try {
            // 只处理充值订单
            if (event.getOrderType() != OrderType.RECHARGE) {
                logger.debug("跳过非充值订单: orderNo={}, orderType={}", event.getOrderNo(), event.getOrderType());
                return;
            }

            logger.info("开始处理余额充值: userId={}, orderNo={}, amount={}", event.getUserId(), event.getOrderNo(),
                    event.getAmount());

            // 构建充值请求
            RechargeRequest rechargeRequest = new RechargeRequest();
            rechargeRequest.setAmount(event.getAmount());
            rechargeRequest.setRemark("订单充值: " + event.getOrderNo());

            // 执行余额充值
            accountAppService.recharge(event.getUserId(), rechargeRequest.getAmount());

            logger.info("余额充值成功: userId={}, orderNo={}, amount={}, title={}", event.getUserId(), event.getOrderNo(),
                    event.getAmount(), event.getTitle());

        } catch (Exception e) {
            logger.error("余额充值失败: userId={}, orderNo={}, amount={}", event.getUserId(), event.getOrderNo(),
                    event.getAmount(), e);

            // 这里可以考虑添加重试机制或者发送告警
            // 由于使用了@Async，异常不会影响主流程
            throw e; // 重新抛出异常，便于Spring的异常处理机制处理
        }
    }
}