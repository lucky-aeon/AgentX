package org.xhy.infrastructure.config;

import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Neo4j事务管理器配置
 * 为Neo4j提供同步事务管理器支持
 * 
 * @author zang
 */
@Configuration
public class Neo4jTransactionConfig {

    /**
     * 创建Neo4j同步事务管理器
     * 用于处理非响应式的Neo4j事务
     */
    @Bean("neo4jTransactionManager")
    public PlatformTransactionManager neo4jTransactionManager(Driver driver) {
        return new Neo4jTransactionManager(driver);
    }
}

