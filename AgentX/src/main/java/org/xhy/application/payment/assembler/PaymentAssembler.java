package org.xhy.application.payment.assembler;

import org.xhy.domain.order.model.OrderEntity;
import org.xhy.infrastructure.payment.model.PaymentResult;
import org.xhy.interfaces.dto.account.response.PaymentResponseDTO;

/** 支付相关对象转换器 */
public class PaymentAssembler {

    /** 转换为支付响应DTO
     * 
     * @param order 订单实体
     * @param paymentResult 支付结果
     * @return 支付响应DTO */
    public static PaymentResponseDTO toPaymentResponseDTO(OrderEntity order, PaymentResult paymentResult) {
        if (order == null || paymentResult == null) {
            return null;
        }

        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setOrderId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setPaymentUrl(paymentResult.getPaymentUrl());
        response.setPaymentMethod(paymentResult.getPaymentMethod());
        response.setPaymentType(paymentResult.getPaymentType());
        response.setAmount(order.getAmount());
        response.setTitle(order.getTitle());
        response.setStatus(String.valueOf(order.getStatus().getCode()));

        return response;
    }

    private PaymentAssembler() {
        // 私有构造函数，防止实例化
    }
}