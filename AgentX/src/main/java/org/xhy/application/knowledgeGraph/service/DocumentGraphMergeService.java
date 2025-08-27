package org.xhy.application.knowledgeGraph.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionRequest;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionRequest.EntityDto;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionRequest.RelationshipDto;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionResponse;
import org.xhy.domain.knowledgeGraph.entity.PageProcessingState;
import org.xhy.domain.knowledgeGraph.service.EntityConflictResolutionService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档级图谱合并服务
 * 负责协调整个文档的分页图谱提取和合并过程
 * 
 * @author shilong.zang
 */
@Service
public class DocumentGraphMergeService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentGraphMergeService.class);

    private final EntityConflictResolutionService conflictResolutionService;
    private final GraphIngestionService graphIngestionService;
    private final DocumentStructuringService documentStructuringService;
    
    // 临时存储每个文档的分页图谱数据（实际项目中应使用Redis或数据库）
    private final Map<String, DocumentGraphBuffer> documentBuffers = new HashMap<>();

    public DocumentGraphMergeService(
            EntityConflictResolutionService conflictResolutionService,
            GraphIngestionService graphIngestionService,
            DocumentStructuringService documentStructuringService) {
        this.conflictResolutionService = conflictResolutionService;
        this.graphIngestionService = graphIngestionService;
        this.documentStructuringService = documentStructuringService;
    }

    /**
     * 处理分页图谱数据
     * 
     * @param request 图谱摄取请求
     * @param pageNumber 页码
     * @param totalPages 总页数
     * @return 处理结果
     */
    @Transactional("neo4jTransactionManager")
    public GraphIngestionResponse processPagedGraph(
            GraphIngestionRequest request, 
            Integer pageNumber, 
            Integer totalPages) {

        String documentId = request.getDocumentId();
        
        logger.info("处理文档 {} 的第 {} 页图谱数据，总页数: {}", documentId, pageNumber, totalPages);

        // 获取或创建文档缓冲区
        DocumentGraphBuffer buffer = getOrCreateDocumentBuffer(documentId, totalPages);

        // 为实体和关系添加页面信息
        addPageInfoToEntities(request.getEntities(), documentId, pageNumber);
        addPageInfoToRelationships(request.getRelationships(), documentId, pageNumber);

        // 将当前页的数据添加到缓冲区
        buffer.addPageData(pageNumber, request.getEntities(), request.getRelationships());

        // 检查是否所有页面都已处理完成
        if (buffer.isAllPagesReceived()) {
            logger.info("文档 {} 所有页面处理完成，开始合并图谱数据", documentId);
            return mergeAndIngestDocumentGraph(documentId, buffer);
        } else {
            logger.info("文档 {} 第 {} 页处理完成，等待其他页面", documentId, pageNumber);
            return GraphIngestionResponse.partial(documentId, 
                    request.getEntities() != null ? request.getEntities().size() : 0,
                    request.getRelationships() != null ? request.getRelationships().size() : 0,
                    "页面数据已缓存，等待其他页面完成");
        }
    }

    /**
     * 合并并摄取文档图谱数据
     */
    private GraphIngestionResponse mergeAndIngestDocumentGraph(String documentId, DocumentGraphBuffer buffer) {
        try {
            // 合并所有页面的实体
            List<EntityDto> allEntities = buffer.getAllEntities();
            List<EntityDto> mergedEntities = conflictResolutionService.mergeConflictingEntities(allEntities);

            // 生成实体合并映射
            Map<String, String> entityMergeMap = conflictResolutionService.generateEntityMergeMap(allEntities, mergedEntities);

            // 合并所有页面的关系并更新实体引用
            List<RelationshipDto> allRelationships = buffer.getAllRelationships();
            List<RelationshipDto> updatedRelationships = conflictResolutionService
                    .updateRelationshipsAfterEntityMerge(allRelationships, entityMergeMap);

            // 使用文档结构化服务创建主节点和层次结构
            GraphIngestionResponse response = documentStructuringService.createDocumentStructure(
                    documentId, mergedEntities, updatedRelationships, buffer.getTotalPages());

            logger.info("文档 {} 图谱合并完成，原始实体数: {}, 合并后实体数: {}, 关系数: {}",
                    documentId, allEntities.size(), mergedEntities.size(), updatedRelationships.size());

            // 清理缓冲区
            documentBuffers.remove(documentId);

            return response;

        } catch (Exception e) {
            logger.error("文档 {} 图谱合并失败: {}", documentId, e.getMessage(), e);
            // 清理缓冲区
            documentBuffers.remove(documentId);
            throw e;
        }
    }

    /**
     * 获取或创建文档缓冲区
     */
    private DocumentGraphBuffer getOrCreateDocumentBuffer(String documentId, Integer totalPages) {
        return documentBuffers.computeIfAbsent(documentId, 
                id -> new DocumentGraphBuffer(id, totalPages));
    }

    /**
     * 为实体添加页面信息
     */
    private void addPageInfoToEntities(List<EntityDto> entities, String fileId, Integer pageNumber) {
        if (entities != null) {
            entities.forEach(entity -> {
                entity.setFileId(fileId);
                entity.setPageNumber(pageNumber);
            });
        }
    }

    /**
     * 为关系添加页面信息
     */
    private void addPageInfoToRelationships(List<RelationshipDto> relationships, String fileId, Integer pageNumber) {
        if (relationships != null) {
            relationships.forEach(relationship -> {
                relationship.setFileId(fileId);
                relationship.setPageNumber(pageNumber);
            });
        }
    }

    /**
     * 处理单页图谱（非分页模式的兼容处理）
     */
    public GraphIngestionResponse processSinglePageGraph(GraphIngestionRequest request) {
        logger.info("处理非分页文档 {} 的图谱数据", request.getDocumentId());
        return graphIngestionService.ingestGraphData(request);
    }

    /**
     * 强制完成文档处理（即使有页面缺失）
     * 用于处理部分页面失败的情况
     */
    public GraphIngestionResponse forceCompleteDocument(String documentId) {
        DocumentGraphBuffer buffer = documentBuffers.get(documentId);
        if (buffer == null) {
            throw new IllegalArgumentException("文档 " + documentId + " 的缓冲区不存在");
        }

        logger.warn("强制完成文档 {} 的处理，可能存在页面缺失", documentId);
        return mergeAndIngestDocumentGraph(documentId, buffer);
    }

    /**
     * 获取文档处理状态
     */
    public PageProcessingState getDocumentProcessingState(String documentId) {
        DocumentGraphBuffer buffer = documentBuffers.get(documentId);
        if (buffer == null) {
            return null;
        }

        PageProcessingState state = new PageProcessingState(documentId, buffer.getTotalPages());
        state.setCompletedPages(new ArrayList<>(buffer.getCompletedPages()));
        
        return state;
    }

    /**
     * 清理文档缓冲区（清理任务或错误恢复时使用）
     */
    public void clearDocumentBuffer(String documentId) {
        documentBuffers.remove(documentId);
        logger.info("已清理文档 {} 的缓冲区", documentId);
    }

    /**
     * 文档图谱缓冲区内部类
     * 用于临时存储分页图谱数据
     */
    private static class DocumentGraphBuffer {
        private final String documentId;
        private final Integer totalPages;
        private final Map<Integer, List<EntityDto>> pageEntities = new HashMap<>();
        private final Map<Integer, List<RelationshipDto>> pageRelationships = new HashMap<>();
        private final Set<Integer> completedPages = new HashSet<>();

        public DocumentGraphBuffer(String documentId, Integer totalPages) {
            this.documentId = documentId;
            this.totalPages = totalPages;
        }

        public void addPageData(Integer pageNumber, List<EntityDto> entities, List<RelationshipDto> relationships) {
            if (entities != null) {
                pageEntities.put(pageNumber, new ArrayList<>(entities));
            }
            if (relationships != null) {
                pageRelationships.put(pageNumber, new ArrayList<>(relationships));
            }
            completedPages.add(pageNumber);
        }

        public boolean isAllPagesReceived() {
            return totalPages != null && completedPages.size() == totalPages;
        }

        public List<EntityDto> getAllEntities() {
            return pageEntities.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }

        public List<RelationshipDto> getAllRelationships() {
            return pageRelationships.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }

        public String getDocumentId() {
            return documentId;
        }

        public Integer getTotalPages() {
            return totalPages;
        }

        public Set<Integer> getCompletedPages() {
            return new HashSet<>(completedPages);
        }
    }
}
