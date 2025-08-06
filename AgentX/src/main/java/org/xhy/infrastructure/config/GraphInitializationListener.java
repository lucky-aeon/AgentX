package org.xhy.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.xhy.application.knowledgeGraph.schema.GraphSchemaManager;

/**
 * 知识图谱初始化监听器
 * 应用启动后自动执行图谱模式初始化
 * 
 * @author zang
 */
@Component
public class GraphInitializationListener {

    private static final Logger logger = LoggerFactory.getLogger(GraphInitializationListener.class);

    private final GraphSchemaManager schemaManager;

    public GraphInitializationListener(GraphSchemaManager schemaManager) {
        this.schemaManager = schemaManager;
    }

    /**
     * 应用启动完成后执行图谱模式初始化
     */
    @EventListener
    @Order(100) // 确保在其他初始化完成后执行
    public void onApplicationReady(ApplicationReadyEvent event) {
        logger.info("AgentX知识图谱服务正在初始化...");
        
        try {
            // 初始化图谱模式
            schemaManager.initializeSchema();
            
            // 记录初始化状态
            var indexStats = schemaManager.getIndexStatistics();
            logger.info("知识图谱服务初始化完成，当前索引数量: {}", indexStats.get("totalIndexes"));
            
        } catch (Exception e) {
            logger.error("知识图谱服务初始化失败: {}", e.getMessage(), e);
            // 不抛出异常，避免影响整个应用启动
        }
    }
}