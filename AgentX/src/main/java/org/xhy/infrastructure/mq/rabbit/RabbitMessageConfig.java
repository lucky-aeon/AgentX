package org.xhy.infrastructure.mq.rabbit;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * RabbitMQ 消息转换器配置
 * 配置 Jackson JSON 消息转换器以支持自动序列化/反序列化
 *
 * @author zang
 * @date 2025-10-17
 */
@EnableRabbit
@Configuration
public class RabbitMessageConfig {

    /**
     * 创建 Jackson JSON 消息转换器
     *
     * @return JSON 消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 注册 Java 8 时间模块
        objectMapper.registerModule(new JavaTimeModule());
        // 禁用时间戳序列化,使用标准日期格式
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * 配置监听器容器工厂
     *
     * @param connectionFactory RabbitMQ 连接工厂
     * @return 监听器容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        // 设置 JSON 消息转换器
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }
}
