package org.xhy.infrastructure.mq.configure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.AbstractConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.Resource;

/**
 * @author zang
 * @date 11:07 <br/>
 */
@Configuration
public class MyRabbitmqConfig {

    private static final Logger log = LoggerFactory.getLogger(MyRabbitmqConfig.class);
    @Resource
    private MyRabbitmqProperties myRabbitmqProp;

    @Resource
    private RabbitProperties rabbitProperties;

    private RabbitTemplate rabbitTemplate;

    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter()); // json转消息
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL); // 手动确认（手动加上的）
        factory.setPrefetchCount(1); // 每次只能获取一条，处理完成才能获取下一条（手动加上的）
        factory.setConcurrentConsumers(rabbitProperties.getListener().getSimple().getConcurrency());// 初始的消费者数量
        factory.setMaxConcurrentConsumers(rabbitProperties.getListener().getSimple().getMaxConcurrency());// 最大的消费者数量
        return factory;
    }

    /**
     * 发送消息进行序列化转换json
     *
     * @return
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }


    //@Primary
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {

        if (connectionFactory instanceof AbstractConnectionFactory) {
            AbstractConnectionFactory abstractConnectionFactory = (AbstractConnectionFactory) connectionFactory;
            // 设置 heartbeat 时间，单位为秒
            abstractConnectionFactory.getRabbitConnectionFactory()
                    .setRequestedHeartbeat(myRabbitmqProp.getRequestedHeartbeat());
            // 启用 Channel 的自动恢复
            abstractConnectionFactory.getRabbitConnectionFactory()
                    .setAutomaticRecoveryEnabled(myRabbitmqProp.isAutomaticRecovery());
            // 设置连接超时时间，单位为毫秒
            abstractConnectionFactory.getRabbitConnectionFactory()
                    .setConnectionTimeout(myRabbitmqProp.getConnectionTimeout());
            //设置网络恢复间隔，以毫秒为单位
            abstractConnectionFactory.getRabbitConnectionFactory()
                    .setNetworkRecoveryInterval(myRabbitmqProp.getNetworkRecoveryInterval());
        }

        if (connectionFactory instanceof CachingConnectionFactory) {
            CachingConnectionFactory cachingConnectionFactory = (CachingConnectionFactory) connectionFactory;
            // 设置 Channel 缓存大小
            cachingConnectionFactory.setChannelCacheSize(myRabbitmqProp.getCacheSize());
            // 设置 heartbeat 时间，单位为秒
            cachingConnectionFactory.getRabbitConnectionFactory()
                    .setRequestedHeartbeat(myRabbitmqProp.getRequestedHeartbeat());
            // 启用 Channel 的自动恢复
            cachingConnectionFactory.getRabbitConnectionFactory()
                    .setAutomaticRecoveryEnabled(myRabbitmqProp.isAutomaticRecovery());
            // 设置连接超时时间，单位为毫秒
            cachingConnectionFactory.getRabbitConnectionFactory()
                    .setConnectionTimeout(myRabbitmqProp.getConnectionTimeout());
            //设置网络恢复间隔，以毫秒为单位
            cachingConnectionFactory.getRabbitConnectionFactory()
                    .setNetworkRecoveryInterval(myRabbitmqProp.getNetworkRecoveryInterval());
        }

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        this.rabbitTemplate = rabbitTemplate;
        rabbitTemplate.setMessageConverter(messageConverter());
        initRabbitTemplate();
        return rabbitTemplate;
    }

    /**
     * 消息发送端的事务控制
     */
    private void initRabbitTemplate() {
        //设置确认回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            /**
             * 1、只要消息抵达Broker就 ack=true
             *
             * correlationData：当前消息的唯一关联数据(这个是消息的唯一id)
             * ack：消息是否成功收到
             * cause：失败的原因
             */
            /**
             *  ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
             *  每个业务每发送一个消息做消息记录来保证防止消息丢失，写的代码很多的场景:
             * 		解决方案:将消息服务做成一个中间件微服务。推荐将发送消息记录和消息的可靠性保证做成一个中间件的服务提供各个服务使用
             *  ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
             *
             * 1、做好消息的确认机制(生产端确认publisher、消费端确认consumer【手动ack】)
             * 2、TODO 每一个发送的消息都在数据库做好记录，定期将失败的消息再次发送一遍【消息表记录表已经建立】
             */
            // TODO 服务器Broker确认收到消息了，修改消息表mq_message中消息状态为成功（3-已抵达），或者处理成功的消息直接删除
            // TODO 服务器收到了消息,服务器Broker将消息持久化了，修改消息表中的消息状态修改为:已成功处理（3-已抵达）或者将处理成功的消息删除。
            log.info("confirm..correlationData[" + correlationData + "]==>ack:[" + ack + "]==>cause:[" + cause + "]");
        });

        //设置消息抵达队列的确认回调
        /**
         * 2、只要消息没有投递给指定的队列，就触发这个失败回调
         *
         * message：投递失败的消息详细信息
         * replyCode：回复的状态码
         * replyText：回复的文本内容
         * exchange：当时这个消息发给哪个交换机
         * routingKey：当时这个消息用哪个路由键
         */
        rabbitTemplate.setReturnsCallback((message) -> {
            log.error("Fail Message[" + message + "]==>replyCode[" + message.getReplyCode() + "]" +
                    "==>replyText[" + message.getReplyText() + "]==>exchange[" + message.getExchange() + "]==>routingKey[" + message.getRoutingKey() + "]");
            log.error("Fail Message[" + message + "]");
            //报错误了。修改数据库当前消息的错误状态--将消息状态修改为错误状态
        });

    }

}
