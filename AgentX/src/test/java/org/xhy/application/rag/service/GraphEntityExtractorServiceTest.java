package org.xhy.application.rag.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.application.knowledgeGraph.service.GraphQueryService;
import org.xhy.application.rag.dto.KgEnhancedRagRequest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** GraphEntityExtractorService 测试类
 * 
 * @author AgentX */
@ExtendWith(MockitoExtension.class)
class GraphEntityExtractorServiceTest {

    private static final Logger log = LoggerFactory.getLogger(GraphEntityExtractorServiceTest.class);

    @Mock
    private GraphQueryService graphQueryService;

    @InjectMocks
    private GraphEntityExtractorService graphEntityExtractorService;

    @Test
    void testExtractEntitiesAndQuery_WithLLMStrategy() {
        // Given
        String question = "如何使用Spring Boot开发微服务架构？";
        KgEnhancedRagRequest.EntityExtractionStrategy strategy = KgEnhancedRagRequest.EntityExtractionStrategy.LLM;
        int maxDepth = 2;
        int maxRelationsPerEntity = 5;

        // When
        GraphEntityExtractorService.EntityExtractionResult result = graphEntityExtractorService
                .extractEntitiesAndQuery(question, strategy, maxDepth, maxRelationsPerEntity);

        // Then
        assertNotNull(result);
        assertNotNull(result.getExtractedEntities());
        log.info("提取到的实体数量: {}", result.getExtractedEntities().size());

        // 打印提取到的实体
        result.getExtractedEntities().forEach(entity -> log.info("实体: {} - 类型: {} - 置信度: {}", entity.getText(),
                entity.getType(), entity.getConfidence()));
    }

    @Test
    void testExtractEntitiesAndQuery_WithKeywordStrategy() {
        // Given
        String question = "如何使用Spring Boot开发微服务架构？";
        KgEnhancedRagRequest.EntityExtractionStrategy strategy = KgEnhancedRagRequest.EntityExtractionStrategy.KEYWORD;
        int maxDepth = 2;
        int maxRelationsPerEntity = 5;

        // When
        GraphEntityExtractorService.EntityExtractionResult result = graphEntityExtractorService
                .extractEntitiesAndQuery(question, strategy, maxDepth, maxRelationsPerEntity);

        // Then
        assertNotNull(result);
        assertNotNull(result.getExtractedEntities());
        log.info("关键词提取到的实体数量: {}", result.getExtractedEntities().size());

        // 打印提取到的实体
        result.getExtractedEntities()
                .forEach(entity -> log.info("关键词实体: {} - 类型: {}", entity.getText(), entity.getType()));
    }

    @Test
    void testExtractJsonFromText() throws Exception {
        // 使用反射访问私有方法进行测试
        java.lang.reflect.Method method = GraphEntityExtractorService.class.getDeclaredMethod("extractJsonFromText",
                String.class);
        method.setAccessible(true);

        // Test case 1: JSON with markdown
        String input1 = "```json\n{\"entities\": [{\"text\": \"Spring Boot\", \"type\": \"TECHNOLOGY\"}]}\n```";
        String result1 = (String) method.invoke(graphEntityExtractorService, input1);
        assertEquals("{\"entities\": [{\"text\": \"Spring Boot\", \"type\": \"TECHNOLOGY\"}]}", result1);

        // Test case 2: Plain JSON
        String input2 = "{\"entities\": [{\"text\": \"微服务\", \"type\": \"CONCEPT\"}]}";
        String result2 = (String) method.invoke(graphEntityExtractorService, input2);
        assertEquals("{\"entities\": [{\"text\": \"微服务\", \"type\": \"CONCEPT\"}]}", result2);

        // Test case 3: Invalid input
        String input3 = "这不是JSON格式的文本";
        String result3 = (String) method.invoke(graphEntityExtractorService, input3);
        assertEquals("{}", result3);

        log.info("JSON提取测试通过");
    }
}
