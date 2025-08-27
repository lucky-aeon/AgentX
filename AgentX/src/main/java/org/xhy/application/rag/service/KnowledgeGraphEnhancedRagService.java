package org.xhy.application.rag.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.application.knowledgeGraph.dto.GraphQueryResponse;
import org.xhy.application.knowledgeGraph.service.GraphQueryService;
import org.xhy.application.rag.assembler.DocumentUnitAssembler;
import org.xhy.application.rag.dto.DocumentUnitDTO;
import org.xhy.application.rag.dto.KgEnhancedRagRequest;
import org.xhy.application.rag.dto.KgEnhancedRagResponse;
import org.xhy.application.rag.dto.RagSearchRequest;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.infrastructure.exception.BusinessException;

/**
 * 知识图谱增强RAG检索应用服务
 * 结合向量搜索和知识图谱查询，提供更准确的检索结果
 * 
 * @author AgentX
 */
@Service
public class KnowledgeGraphEnhancedRagService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGraphEnhancedRagService.class);

    private final RagQaDatasetAppService ragQaDatasetAppService;
    private final GraphQueryService graphQueryService;
    private final GraphEntityExtractorService entityExtractorService;
    private final HybridSearchStrategy hybridSearchStrategy;

    public KnowledgeGraphEnhancedRagService(RagQaDatasetAppService ragQaDatasetAppService, 
                                          GraphQueryService graphQueryService,
                                          GraphEntityExtractorService entityExtractorService,
                                          HybridSearchStrategy hybridSearchStrategy) {
        this.ragQaDatasetAppService = ragQaDatasetAppService;
        this.graphQueryService = graphQueryService;
        this.entityExtractorService = entityExtractorService;
        this.hybridSearchStrategy = hybridSearchStrategy;
    }

    /**
     * 执行知识图谱增强的RAG检索
     * 
     * @param request 增强RAG检索请求
     * @param userId 用户ID
     * @return 增强RAG检索响应
     */
    public KgEnhancedRagResponse enhancedRagSearch(KgEnhancedRagRequest request, String userId) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("开始执行知识图谱增强RAG检索，用户: {}, 查询: '{}'", userId, request.getQuestion());

            // 1. 参数验证
            validateRequest(request, userId);

            // 2. 执行向量搜索
            VectorSearchResult vectorSearchResult = executeVectorSearch(request, userId);
            log.debug("向量搜索返回 {} 个结果，耗时: {}ms", 
                vectorSearchResult.getResults().size(), vectorSearchResult.getSearchTime());

            // 3. 执行图谱查询（如果启用）
            GraphQueryResult graphQueryResult = executeGraphQuery(request);
            if (graphQueryResult != null) {
                log.debug("图谱查询返回 {} 个实体, {} 个关系，耗时: {}ms", 
                    graphQueryResult.getNodes().size(), 
                    graphQueryResult.getRelationships().size(),
                    graphQueryResult.getQueryTime());
            }

            // 4. 融合搜索结果
            FusionResult fusionResult = fuseSearchResults(vectorSearchResult, graphQueryResult, request);
            log.debug("结果融合完成，生成 {} 个增强结果，耗时: {}ms",
                fusionResult.getEnhancedResults().size(), fusionResult.getFusionTime());

            // 5. 重排序和过滤
            List<KgEnhancedRagResponse.EnhancedResult> finalResults = rerankAndFilter(
                fusionResult.getEnhancedResults(), request);

            // 6. 构建响应
            KgEnhancedRagResponse response = buildResponse(
                finalResults, vectorSearchResult, graphQueryResult, fusionResult, startTime);

            log.info("知识图谱增强RAG检索完成，最终返回 {} 个结果，处理时间: {}ms", 
                finalResults.size(), response.getProcessingTimeMs());

            return response;

        } catch (Exception e) {
            log.error("知识图谱增强RAG检索失败", e);
            throw new BusinessException("增强RAG检索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 参数验证
     */
    private void validateRequest(KgEnhancedRagRequest request, String userId) {
        if (request == null) {
            throw new BusinessException("请求参数不能为空");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new BusinessException("用户ID不能为空");
        }
        if (request.getDatasetIds() == null || request.getDatasetIds().isEmpty()) {
            throw new BusinessException("数据集ID列表不能为空");
        }
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            throw new BusinessException("查询问题不能为空");
        }
    }

    /**
     * 执行向量搜索
     */
    private VectorSearchResult executeVectorSearch(KgEnhancedRagRequest request, String userId) {
        long startTime = System.currentTimeMillis();
        
        try {
            List<DocumentUnitEntity> vectorResults = performVectorSearch(request, userId);
            long searchTime = System.currentTimeMillis() - startTime;
            
            return new VectorSearchResult(vectorResults, searchTime);
        } catch (Exception e) {
            log.error("向量搜索执行失败", e);
            throw new BusinessException("向量搜索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行图谱查询
     */
    private GraphQueryResult executeGraphQuery(KgEnhancedRagRequest request) {
        if (!Boolean.TRUE.equals(request.getEnableGraphEnhancement())) {
            return null;
        }

        long startTime = System.currentTimeMillis();
        
        try {
            GraphEntityExtractorService.EntityExtractionResult extractionResult = 
                entityExtractorService.extractEntitiesAndQuery(
                    request.getQuestion(), 
                    request.getEntityExtractionStrategy(),
                    request.getMaxGraphDepth(),
                    request.getMaxRelationsPerEntity()
                );
            
            long queryTime = System.currentTimeMillis() - startTime;
            
            // 将ExtractedEntity转换为String列表
            List<String> extractedEntityTexts = extractionResult.getExtractedEntities().stream()
                .map(GraphEntityExtractorService.ExtractedEntity::getText)
                .collect(Collectors.toList());
            
            return new GraphQueryResult(
                extractionResult.getGraphNodes(),
                extractionResult.getGraphRelationships(),
                extractedEntityTexts,
                extractionResult.getQueryCount(),
                queryTime
            );
        } catch (Exception e) {
            log.warn("图谱查询执行失败，将跳过图谱增强: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 融合搜索结果
     */
    private FusionResult fuseSearchResults(VectorSearchResult vectorResult, 
                                         GraphQueryResult graphResult, 
                                         KgEnhancedRagRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            List<KgEnhancedRagResponse.EnhancedResult> enhancedResults = hybridSearchStrategy.fuseResults(
                vectorResult.getResults(),
                graphResult != null ? graphResult.getNodes() : new ArrayList<>(),
                graphResult != null ? graphResult.getRelationships() : new ArrayList<>(),
                request
            );
            
            long fusionTime = System.currentTimeMillis() - startTime;
            
            return new FusionResult(enhancedResults, fusionTime);
        } catch (Exception e) {
            log.error("结果融合失败", e);
            throw new BusinessException("结果融合失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建响应对象
     */
    private KgEnhancedRagResponse buildResponse(List<KgEnhancedRagResponse.EnhancedResult> finalResults,
                                              VectorSearchResult vectorResult,
                                              GraphQueryResult graphResult,
                                              FusionResult fusionResult,
                                              long startTime) {
        KgEnhancedRagResponse response = new KgEnhancedRagResponse();
        response.setResults(finalResults);
        response.setVectorResultCount(vectorResult.getResults().size());
        response.setGraphEntityCount(graphResult != null ? graphResult.getNodes().size() : 0);
        response.setGraphRelationshipCount(graphResult != null ? graphResult.getRelationships().size() : 0);
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        response.setSuccess(true);

        // 设置详细统计信息
        KgEnhancedRagResponse.SearchStatistics stats = new KgEnhancedRagResponse.SearchStatistics();
        stats.setTotalQueryTime(response.getProcessingTimeMs());
        stats.setVectorSearchTime(vectorResult.getSearchTime());
        stats.setGraphQueryTime(graphResult != null ? graphResult.getQueryTime() : 0);
        stats.setFusionTime(fusionResult.getFusionTime());
        stats.setRerankTime(0); // 重排时间在rerankAndFilter方法中计算
        stats.setExtractedEntitiesCount(graphResult != null ? graphResult.getExtractedEntities().size() : 0);
        stats.setGraphQueryCount(graphResult != null ? graphResult.getQueryCount() : 0);
        response.setStatistics(stats);

        return response;
    }

    /**
     * 执行传统向量RAG搜索
     */
    private List<DocumentUnitEntity> performVectorSearch(KgEnhancedRagRequest request, String userId) {
        try {
            // 构建RAG搜索请求
            RagSearchRequest ragRequest = new RagSearchRequest();
            ragRequest.setDatasetIds(request.getDatasetIds());
            ragRequest.setQuestion(request.getQuestion());
            ragRequest.setMaxResults(request.getMaxResults() * 2); // 获取更多候选结果用于融合
            ragRequest.setMinScore(request.getMinScore());
            ragRequest.setEnableRerank(request.getEnableRerank());
            ragRequest.setCandidateMultiplier(request.getCandidateMultiplier());
            ragRequest.setEnableQueryExpansion(request.getEnableQueryExpansion());

            // 执行向量搜索
            List<DocumentUnitDTO> dtos = ragQaDatasetAppService.ragSearch(ragRequest, userId);
            return DocumentUnitAssembler.toEntities(dtos);

        } catch (Exception e) {
            log.error("向量RAG搜索失败", e);
            throw new BusinessException("向量搜索失败: " + e.getMessage(), e);
        }
    }





    /**
     * 重排序和过滤结果
     */
    private List<KgEnhancedRagResponse.EnhancedResult> rerankAndFilter(
            List<KgEnhancedRagResponse.EnhancedResult> results,
            KgEnhancedRagRequest request) {
        
        try {
            log.debug("开始重排序和过滤，原始结果数: {}", results.size());
            
            // 保存原始结果副本，用于宽松过滤
            List<KgEnhancedRagResponse.EnhancedResult> originalResults = new ArrayList<>(results);
            
            // 1. 多维度排序
            results.sort((a, b) -> {
                // 首先按相关性评分排序
                int scoreCompare = Double.compare(b.getRelevanceScore(), a.getRelevanceScore());
                if (scoreCompare != 0) {
                    return scoreCompare;
                }
                
                // 相关性评分相同时，按图谱增强程度排序
                int graphEntityCountA = a.getGraphEntities() != null ? a.getGraphEntities().size() : 0;
                int graphEntityCountB = b.getGraphEntities() != null ? b.getGraphEntities().size() : 0;
                int graphCompare = Integer.compare(graphEntityCountB, graphEntityCountA);
                if (graphCompare != 0) {
                    return graphCompare;
                }
                
                // 最后按向量评分排序
                double vectorScoreA = a.getVectorScore() != null ? a.getVectorScore() : 0.0;
                double vectorScoreB = b.getVectorScore() != null ? b.getVectorScore() : 0.0;
                return Double.compare(vectorScoreB, vectorScoreA);
            });
            
            // 2. 应用多样性过滤（避免过于相似的结果）
            if (request.getEnableRerank() != null && request.getEnableRerank()) {
                results = applyDiversityFilter(results);
            }
            
            // 3. 过滤低分结果
            double minThreshold = request.getMinScore() != null ? request.getMinScore() : 0.3;
            log.debug("应用最小分数阈值: {}，过滤前结果数: {}", minThreshold, results.size());
            
            List<KgEnhancedRagResponse.EnhancedResult> filteredResults = results.stream()
                .filter(result -> {
                    // 对于图谱增强的结果，使用更宽松的阈值
                    boolean hasGraphEnhancement = result.getGraphEntities() != null && !result.getGraphEntities().isEmpty();
                    boolean isGraphOnlyResult = "GRAPH".equals(result.getSourceType());
                    
                    double effectiveThreshold;
                    if (isGraphOnlyResult || hasGraphEnhancement) {
                        // 对于纯图谱结果或图谱增强结果，使用更宽松的阈值
                        effectiveThreshold = Math.min(minThreshold, 0.2);
                    } else {
                        effectiveThreshold = minThreshold;
                    }
                    
                    boolean passThreshold = result.getRelevanceScore() >= effectiveThreshold;
                    
                    log.debug("评估结果：sourceType={}, relevanceScore={}, effectiveThreshold={}, hasGraphEnhancement={}, isGraphOnlyResult={}, 通过={}", 
                        result.getSourceType(), result.getRelevanceScore(), effectiveThreshold, hasGraphEnhancement, isGraphOnlyResult, passThreshold);
                    
                    if (!passThreshold) {
                        log.debug("结果被过滤：相关性分数 {} 低于阈值 {} (图谱增强: {}, 纯图谱: {})", 
                            result.getRelevanceScore(), effectiveThreshold, hasGraphEnhancement, isGraphOnlyResult);
                    }
                    return passThreshold;
                })
                .collect(Collectors.toList());
            
            log.debug("过滤后结果数: {}", filteredResults.size());
            results = filteredResults;
            
            // 如果过滤后没有结果，使用更宽松的策略
            if (results.isEmpty() && !originalResults.isEmpty()) {
                log.warn("严格过滤后无结果，使用更宽松的阈值重试，原始阈值: {}", minThreshold);
                double relaxedThreshold = Math.min(minThreshold * 0.5, 0.1);
                
                // 对原始结果应用排序
                originalResults.sort((a, b) -> {
                    int scoreCompare = Double.compare(b.getRelevanceScore(), a.getRelevanceScore());
                    if (scoreCompare != 0) return scoreCompare;
                    
                    int graphEntityCountA = a.getGraphEntities() != null ? a.getGraphEntities().size() : 0;
                    int graphEntityCountB = b.getGraphEntities() != null ? b.getGraphEntities().size() : 0;
                    int graphCompare = Integer.compare(graphEntityCountB, graphEntityCountA);
                    if (graphCompare != 0) return graphCompare;
                    
                    double vectorScoreA = a.getVectorScore() != null ? a.getVectorScore() : 0.0;
                    double vectorScoreB = b.getVectorScore() != null ? b.getVectorScore() : 0.0;
                    return Double.compare(vectorScoreB, vectorScoreA);
                });
                
                // 应用宽松过滤
                results = originalResults.stream()
                    .filter(result -> result.getRelevanceScore() >= relaxedThreshold)
                    .collect(Collectors.toList());
                log.debug("宽松过滤后结果数: {}", results.size());
            }
            
            // 4. 限制结果数量
            int maxResults = request.getMaxResults() != null ? request.getMaxResults() : 15;
            if (results.size() > maxResults) {
                results = results.subList(0, maxResults);
            }
            
            log.debug("重排序和过滤完成，最终结果数: {}", results.size());
            return results;
            
        } catch (Exception e) {
            log.warn("重排序过程中发生错误，使用原始排序", e);
            // 发生错误时使用简单排序
            results.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));
            int maxResults = request.getMaxResults() != null ? request.getMaxResults() : 15;
            return results.size() > maxResults ? results.subList(0, maxResults) : results;
        }
    }
    
    /**
     * 应用多样性过滤，避免返回过于相似的结果
     */
    private List<KgEnhancedRagResponse.EnhancedResult> applyDiversityFilter(
            List<KgEnhancedRagResponse.EnhancedResult> results) {
        
        if (results.size() <= 3) {
            return results; // 结果太少，不需要多样性过滤
        }
        
        List<KgEnhancedRagResponse.EnhancedResult> diverseResults = new ArrayList<>();
        Set<String> seenContent = new HashSet<>();
        
        for (KgEnhancedRagResponse.EnhancedResult result : results) {
            if (diverseResults.size() >= results.size() * 0.8) {
                break; // 保留80%的结果
            }
            
            // 检查内容相似性
            if (result.getDocumentUnit() != null && result.getDocumentUnit().getContent() != null) {
                String content = result.getDocumentUnit().getContent();
                String contentSignature = generateContentSignature(content);
                
                if (!seenContent.contains(contentSignature)) {
                    seenContent.add(contentSignature);
                    diverseResults.add(result);
                }
            } else {
                // 图谱结果或无内容的结果直接添加
                diverseResults.add(result);
            }
        }
        
        // 如果过滤后结果太少，补充一些原始结果
        if (diverseResults.size() < Math.min(5, results.size() / 2)) {
            for (KgEnhancedRagResponse.EnhancedResult result : results) {
                if (!diverseResults.contains(result) && diverseResults.size() < results.size() / 2) {
                    diverseResults.add(result);
                }
            }
        }
        
        return diverseResults;
    }
    
    /**
     * 生成内容签名用于相似性检测
     */
    private String generateContentSignature(String content) {
        if (content == null || content.length() < 50) {
            return content != null ? content : "";
        }
        
        // 使用前50个字符和后50个字符作为内容签名
        String prefix = content.substring(0, Math.min(50, content.length()));
        String suffix = content.length() > 50 ? 
            content.substring(Math.max(0, content.length() - 50)) : "";
        
        return prefix + "|" + suffix;
    }

    /**
     * 向量搜索结果内部类
     */
    private static class VectorSearchResult {
        private final List<DocumentUnitEntity> results;
        private final long searchTime;

        public VectorSearchResult(List<DocumentUnitEntity> results, long searchTime) {
            this.results = results != null ? results : new ArrayList<>();
            this.searchTime = searchTime;
        }

        public List<DocumentUnitEntity> getResults() {
            return results;
        }

        public long getSearchTime() {
            return searchTime;
        }
    }

    /**
     * 图谱查询结果内部类
     */
    private static class GraphQueryResult {
        private final List<GraphQueryResponse.NodeResult> nodes;
        private final List<GraphQueryResponse.RelationshipResult> relationships;
        private final List<String> extractedEntities;
        private final int queryCount;
        private final long queryTime;

        public GraphQueryResult(List<GraphQueryResponse.NodeResult> nodes,
                              List<GraphQueryResponse.RelationshipResult> relationships,
                              List<String> extractedEntities,
                              int queryCount,
                              long queryTime) {
            this.nodes = nodes != null ? nodes : new ArrayList<>();
            this.relationships = relationships != null ? relationships : new ArrayList<>();
            this.extractedEntities = extractedEntities != null ? extractedEntities : new ArrayList<>();
            this.queryCount = queryCount;
            this.queryTime = queryTime;
        }

        public List<GraphQueryResponse.NodeResult> getNodes() {
            return nodes;
        }

        public List<GraphQueryResponse.RelationshipResult> getRelationships() {
            return relationships;
        }

        public List<String> getExtractedEntities() {
            return extractedEntities;
        }

        public int getQueryCount() {
            return queryCount;
        }

        public long getQueryTime() {
            return queryTime;
        }
    }

    /**
     * 融合结果内部类
     */
    private static class FusionResult {
        private final List<KgEnhancedRagResponse.EnhancedResult> enhancedResults;
        private final long fusionTime;

        public FusionResult(List<KgEnhancedRagResponse.EnhancedResult> enhancedResults, long fusionTime) {
            this.enhancedResults = enhancedResults != null ? enhancedResults : new ArrayList<>();
            this.fusionTime = fusionTime;
        }

        public List<KgEnhancedRagResponse.EnhancedResult> getEnhancedResults() {
            return enhancedResults;
        }

        public long getFusionTime() {
            return fusionTime;
        }
    }
}
