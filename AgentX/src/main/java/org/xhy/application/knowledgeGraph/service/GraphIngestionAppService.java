package org.xhy.application.knowledgeGraph.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionRequest;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionResponse;
import org.xhy.domain.knowledgeGraph.service.KnowledgeGraphDomainService;
import org.xhy.infrastructure.exception.BusinessException;

/** 知识图谱数据摄取应用服务 负责协调知识图谱数据的批量处理和存储业务流程
 * 
 * @author zang
 * @since 1.0.0 */
@Service
public class GraphIngestionAppService {

    private static final Logger logger = LoggerFactory.getLogger(GraphIngestionAppService.class);

    private final KnowledgeGraphDomainService knowledgeGraphDomainService;

    /** 构造函数
     * 
     * @param knowledgeGraphDomainService 知识图谱领域服务 */
    public GraphIngestionAppService(KnowledgeGraphDomainService knowledgeGraphDomainService) {
        this.knowledgeGraphDomainService = knowledgeGraphDomainService;
    }

    /** 摄取知识图谱数据 协调知识图谱数据的摄取流程，包括数据验证和批量处理
     * 
     * @param request 图数据摄取请求，包含实体和关系数据
     * @return 摄取结果响应，包含处理统计信息
     * @throws BusinessException 当请求参数无效或处理失败时抛出 */
    @Transactional
    public GraphIngestionResponse ingestGraphData(GraphIngestionRequest request) {
        if (request == null) {
            throw new BusinessException("摄取请求不能为空");
        }

        if (request.getDocumentId() == null || request.getDocumentId().trim().isEmpty()) {
            throw new BusinessException("文档ID不能为空");
        }

        long startTime = System.currentTimeMillis();

        try {
            logger.info("Start graph data ingestion for document: {}", request.getDocumentId());

            // 检查是否有数据要处理
            boolean hasEntities = request.getEntities() != null && !request.getEntities().isEmpty();
            boolean hasRelationships = request.getRelationships() != null && !request.getRelationships().isEmpty();

            if (!hasEntities && !hasRelationships) {
                return GraphIngestionResponse.success(request.getDocumentId(), 0, 0, "无数据需要摄取");
            }

            // 委托给领域服务处理具体的数据摄取逻辑
            GraphIngestionResponse response = knowledgeGraphDomainService.ingestGraphData(request);
            response.setProcessingTime(startTime);

            logger.info("Graph data ingestion completed for document: {}, processing time: {}ms",
                    request.getDocumentId(), response.getProcessingTimeMs());

            return response;

        } catch (Exception e) {
            logger.error("Graph data ingestion failed for document: {}, error: {}", request.getDocumentId(),
                    e.getMessage(), e);
            throw new BusinessException("图数据摄取失败: " + e.getMessage(), e);
        }
    }

}