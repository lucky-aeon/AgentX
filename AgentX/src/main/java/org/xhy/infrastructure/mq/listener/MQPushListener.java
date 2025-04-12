package org.xhy.infrastructure.mq.listener;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.xhy.infrastructure.mq.model.MQSendEventModel;
import org.xhy.infrastructure.mq.utils.RabbitMQUtils;

import jakarta.annotation.Resource;

/**
 * @author shilong.zang
 * @date 20:38 <br/>
 */
@Component
public class MQPushListener implements ApplicationListener<MQSendEventModel<?>> {

    private static final Logger log = LoggerFactory.getLogger(MQPushListener.class);

    @Resource
    private RabbitMQUtils rabbitMQUtils;

    @Override
    public void onApplicationEvent(@NotNull MQSendEventModel<?> event) {
        // do something
        log.info("consume mq event:{}", event);
        log.info("consume mq event body:{}", event.getMsgBody());
        rabbitMQUtils.pushMsg(event);
    }

}
