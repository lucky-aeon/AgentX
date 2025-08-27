package org.xhy.application.knowledgeGraph.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionRequest;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionResponse;
import org.xhy.domain.knowledgeGraph.entity.PageProcessingState;
import org.xhy.domain.knowledgeGraph.message.DocIeInferMessage;

/**
 * 分页图谱处理协调器
 * 负责协调分页和非分页图谱提取的处理流程
 * 
 * @author shilong.zang
 */
@Service
public class PagedGraphProcessingOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(PagedGraphProcessingOrchestrator.class);

    private final DocumentGraphMergeService documentGraphMergeService;

    public PagedGraphProcessingOrchestrator(DocumentGraphMergeService documentGraphMergeService) {
        this.documentGraphMergeService = documentGraphMergeService;
    }

    /**
     * 协调图谱处理流程
     * 根据消息类型决定使用分页处理还是单页处理
     * 
     * @param message 文档处理消息
     * @param extractedGraph 提取的图谱数据
     * @return 处理结果
     */
    public GraphIngestionResponse orchestrateGraphProcessing(
            DocIeInferMessage message, 
            GraphIngestionRequest extractedGraph) {

        if (message.isPaged()) {
            // 分页处理模式
            return processPagedDocument(message, extractedGraph);
        } else {
            // 单页处理模式（向后兼容）
            return processSinglePageDocument(extractedGraph);
        }
    }

    /**
     * 处理分页文档
     */
    private GraphIngestionResponse processPagedDocument(
            DocIeInferMessage message, 
            GraphIngestionRequest extractedGraph) {

        String documentId = message.getFileId();
        Integer pageNumber = message.getPageNumber();
        Integer totalPages = message.getTotalPages();

        logger.info("处理分页文档 {}, 页码: {}/{}", documentId, pageNumber, totalPages);

        try {
            // 设置图谱请求的文档ID（确保一致性）
            extractedGraph.setDocumentId(documentId);

            // 委托给文档图谱合并服务处理
            GraphIngestionResponse response = documentGraphMergeService.processPagedGraph(
                    extractedGraph, pageNumber, totalPages);

            // 记录处理结果
            if (response.isSuccess()) {
                if (message.isLastPage()) {
                    logger.info("文档 {} 最后一页处理完成", documentId);
                } else {
                    logger.info("文档 {} 第 {} 页处理完成，等待其他页面", documentId, pageNumber);
                }
            } else {
                logger.error("文档 {} 第 {} 页处理失败: {}", documentId, pageNumber, response.getMessage());
            }

            return response;

        } catch (Exception e) {
            logger.error("处理分页文档 {} 第 {} 页时发生异常: {}", documentId, pageNumber, e.getMessage(), e);
            
            // 返回失败响应
            return GraphIngestionResponse.error(documentId, 
                    "分页文档处理失败: " + e.getMessage());
        }
    }

    /**
     * 处理单页文档
     */
    private GraphIngestionResponse processSinglePageDocument(GraphIngestionRequest extractedGraph) {
        String documentId = extractedGraph.getDocumentId();
        
        logger.info("处理单页文档 {}", documentId);

        try {
            // 直接处理单页图谱
            GraphIngestionResponse response = documentGraphMergeService.processSinglePageGraph(extractedGraph);

            if (response.isSuccess()) {
                logger.info("单页文档 {} 处理完成", documentId);
            } else {
                logger.error("单页文档 {} 处理失败: {}", documentId, response.getMessage());
            }

            return response;

        } catch (Exception e) {
            logger.error("处理单页文档 {} 时发生异常: {}", documentId, e.getMessage(), e);
            
            // 返回失败响应
            return GraphIngestionResponse.error(documentId, 
                    "单页文档处理失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档处理状态
     * 
     * @param documentId 文档ID
     * @return 处理状态
     */
    public PageProcessingState getDocumentProcessingState(String documentId) {
        return documentGraphMergeService.getDocumentProcessingState(documentId);
    }

    /**
     * 强制完成文档处理
     * 当某些页面处理失败但需要处理剩余页面时使用
     * 
     * @param documentId 文档ID
     * @return 处理结果
     */
    public GraphIngestionResponse forceCompleteDocument(String documentId) {
        logger.warn("强制完成文档 {} 的处理", documentId);
        
        try {
            return documentGraphMergeService.forceCompleteDocument(documentId);
        } catch (Exception e) {
            logger.error("强制完成文档 {} 处理失败: {}", documentId, e.getMessage(), e);
            return GraphIngestionResponse.error(documentId, 
                    "强制完成文档处理失败: " + e.getMessage());
        }
    }

    /**
     * 清理文档处理状态
     * 用于清理任务或错误恢复
     * 
     * @param documentId 文档ID
     */
    public void clearDocumentProcessingState(String documentId) {
        logger.info("清理文档 {} 的处理状态", documentId);
        documentGraphMergeService.clearDocumentBuffer(documentId);
    }

    /**
     * 验证分页消息的完整性
     * 
     * @param message 文档消息
     * @return 验证结果
     */
    public boolean validatePagedMessage(DocIeInferMessage message) {
        if (!message.isPaged()) {
            return true; // 非分页消息无需验证
        }

        Integer pageNumber = message.getPageNumber();
        Integer totalPages = message.getTotalPages();

        if (pageNumber == null || totalPages == null) {
            logger.error("分页消息缺少必要的分页信息: pageNumber={}, totalPages={}", pageNumber, totalPages);
            return false;
        }

        if (pageNumber < 1 || pageNumber > totalPages) {
            logger.error("分页消息页码无效: pageNumber={}, totalPages={}", pageNumber, totalPages);
            return false;
        }

        if (totalPages < 1) {
            logger.error("分页消息总页数无效: totalPages={}", totalPages);
            return false;
        }

        return true;
    }

    /**
     * 检查是否应该使用分页处理
     * 可以基于文档大小、页数等因素决定
     * 
     * @param message 文档消息
     * @return 是否使用分页处理
     */
    public boolean shouldUsePagedProcessing(DocIeInferMessage message) {
        // 如果消息已经包含分页信息，则使用分页处理
        if (message.isPaged()) {
            return true;
        }

        // 可以基于文档文本长度等因素决定是否需要分页处理
        String documentText = message.getDocumentText();
        if (documentText != null && documentText.length() > 10000) { // 10KB阈值
            logger.info("文档 {} 文本长度超过阈值，建议使用分页处理", message.getFileId());
            return true;
        }

        return false;
    }

    /**
     * 生成处理摘要
     * 
     * @param documentId 文档ID
     * @return 处理摘要
     */
    public String generateProcessingSummary(String documentId) {
        PageProcessingState state = getDocumentProcessingState(documentId);
        
        if (state == null) {
            return "文档 " + documentId + " 未找到处理状态";
        }

        return String.format("文档 %s 处理状态: %s, 总页数: %d, 已完成: %d页, 失败: %d页, 剩余: %d页",
                documentId,
                state.getStatus().getDescription(),
                state.getTotalPages(),
                state.getCompletedPages() != null ? state.getCompletedPages().size() : 0,
                state.getFailedPages() != null ? state.getFailedPages().size() : 0,
                state.getRemainingPages());
    }
}
