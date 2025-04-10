package org.xhy.infrastructure.starter.model;

import org.springframework.amqp.core.ExchangeTypes;

/**
 * 广播MQ消息发送事件
 * @author zang
 * @date 14:20 <br/>
 */
public abstract class MQFanoutSendEventModel<T> extends MQSendEventModel<T> {

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private String description = "base info cud event";

    public MQFanoutSendEventModel(T data) {
        super(data);
    }

    @Override
    public String exchangeType() {
        return ExchangeTypes.FANOUT;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String exchangeName() {
        return "fanout.plss.record.base.info.exchange";
    }


    @Override
    public String routeKey() {
        return "fanout.record.base.info";
    }


}
