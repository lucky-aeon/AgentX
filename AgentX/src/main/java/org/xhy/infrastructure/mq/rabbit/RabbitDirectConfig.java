package org.xhy.infrastructure.mq.rabbit;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xhy.infrastructure.mq.core.MessagePublisher;

/**
 * Minimal RabbitMQ client configuration using the raw client.
 */
@Configuration
public class RabbitDirectConfig {

    @Bean(destroyMethod = "close")
    public Connection rabbitConnection(RabbitProperties props) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(props.getHost());
        if (props.getPort() != null) {
            factory.setPort(props.getPort());
        }
        if (props.getUsername() != null) {
            factory.setUsername(props.getUsername());
        }
        if (props.getPassword() != null) {
            factory.setPassword(props.getPassword());
        }
        if (props.getVirtualHost() != null) {
            factory.setVirtualHost(props.getVirtualHost());
        }
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(10_000);
        return factory.newConnection("agentx-direct-publisher");
    }

    @Bean
    public MessagePublisher messagePublisher(Connection connection) {
        return new RabbitDirectPublisher(connection);
    }
}

