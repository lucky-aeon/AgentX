package org.xhy.application.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.application.rag.dto.KgEnhancedRagRequest;

/** LLM实体提取使用示例 展示如何使用新实现的LLM实体提取功能
 * 
 * @author AgentX */
@Component
public class LLMEntityExtractionExample {

    private static final Logger log = LoggerFactory.getLogger(LLMEntityExtractionExample.class);

    private final GraphEntityExtractorService graphEntityExtractorService;

    public LLMEntityExtractionExample(GraphEntityExtractorService graphEntityExtractorService) {
        this.graphEntityExtractorService = graphEntityExtractorService;
    }

    /** 演示LLM实体提取功能 */
    public void demonstrateLLMEntityExtraction() {
        log.info("=== LLM实体提取功能演示 ===");

        // 测试用例1：技术相关查询
        String query1 = "如何使用Spring Boot和Redis构建高性能的微服务架构？";
        demonstrateExtraction(query1, "技术架构查询");

        // 测试用例2：业务相关查询
        String query2 = "阿里巴巴公司的云计算业务发展如何？";
        demonstrateExtraction(query2, "业务发展查询");

        // 测试用例3：混合查询
        String query3 = "张三在北京的人工智能项目中使用了哪些深度学习框架？";
        demonstrateExtraction(query3, "混合信息查询");

        log.info("=== 演示完成 ===");
    }

    private void demonstrateExtraction(String query, String description) {
        log.info("\n--- {} ---", description);
        log.info("查询文本: {}", query);

        try {
            // 使用LLM策略进行实体提取
            GraphEntityExtractorService.EntityExtractionResult result = graphEntityExtractorService
                    .extractEntitiesAndQuery(query, KgEnhancedRagRequest.EntityExtractionStrategy.LLM, 2, // maxDepth
                            5 // maxRelationsPerEntity
                    );

            log.info("提取结果:");
            log.info("- 处理时间: {} ms", result.getProcessingTimeMs());
            log.info("- 提取实体数: {}", result.getExtractedEntities().size());
            log.info("- 图谱节点数: {}", result.getGraphNodes().size());
            log.info("- 图谱关系数: {}", result.getGraphRelationships().size());

            // 详细显示提取的实体
            if (!result.getExtractedEntities().isEmpty()) {
                log.info("提取的实体详情:");
                result.getExtractedEntities().forEach(entity -> log.info("  * {} [{}] (置信度: {:.2f})", entity.getText(),
                        entity.getType(), entity.getConfidence()));
            }

        } catch (Exception e) {
            log.error("实体提取失败: {}", e.getMessage(), e);
        }
    }

    /** 比较不同提取策略的效果 */
    public void compareExtractionStrategies(String query) {
        log.info("\n=== 提取策略比较 ===");
        log.info("查询文本: {}", query);

        // 测试三种策略
        KgEnhancedRagRequest.EntityExtractionStrategy[] strategies = {
                KgEnhancedRagRequest.EntityExtractionStrategy.KEYWORD,
                KgEnhancedRagRequest.EntityExtractionStrategy.NER, KgEnhancedRagRequest.EntityExtractionStrategy.LLM};

        for (KgEnhancedRagRequest.EntityExtractionStrategy strategy : strategies) {
            try {
                long startTime = System.currentTimeMillis();

                GraphEntityExtractorService.EntityExtractionResult result = graphEntityExtractorService
                        .extractEntitiesAndQuery(query, strategy, 2, 5);

                long duration = System.currentTimeMillis() - startTime;

                log.info("\n{} 策略结果:", strategy);
                log.info("- 耗时: {} ms", duration);
                log.info("- 实体数: {}", result.getExtractedEntities().size());

                result.getExtractedEntities()
                        .forEach(entity -> log.info("  * {} [{}]", entity.getText(), entity.getType()));

            } catch (Exception e) {
                log.error("{} 策略执行失败: {}", strategy, e.getMessage());
            }
        }
    }
}
