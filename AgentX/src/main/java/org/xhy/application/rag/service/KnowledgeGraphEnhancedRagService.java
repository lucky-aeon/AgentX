package org.xhy.application.rag.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.application.knowledgeGraph.service.GraphQueryService;
import org.xhy.application.rag.assembler.DocumentUnitAssembler;
import org.xhy.application.rag.dto.DocumentUnitDTO;
import org.xhy.application.rag.dto.KgEnhancedRagRequest;
import org.xhy.application.rag.dto.KgEnhancedRagResponse;
import org.xhy.application.rag.dto.RagSearchRequest;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.infrastructure.exception.BusinessException;

/**
 * 知识图谱增强RAG检索服务
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
        long vectorSearchTime = 0;
        long graphQueryTime = 0;
        long fusionTime = 0;
        long rerankTime = 0;
        
        try {
            log.info("开始执行知识图谱增强RAG检索，用户: {}, 查询: '{}'", userId, request.getQuestion());

            // 1. 执行传统向量RAG搜索
            long vectorStart = System.currentTimeMillis();
            List<DocumentUnitEntity> vectorResults = performVectorSearch(request, userId);
            vectorSearchTime = System.currentTimeMillis() - vectorStart;
            log.debug("向量搜索返回 {} 个结果，耗时: {}ms", vectorResults.size(), vectorSearchTime);

            // 2. 提取查询中的实体并执行图谱查询
            GraphEntityExtractorService.EntityExtractionResult extractionResult = null;
            if (request.getEnableGraphEnhancement()) {
                long graphStart = System.currentTimeMillis();
                extractionResult = entityExtractorService.extractEntitiesAndQuery(
                    request.getQuestion(), 
                    request.getEntityExtractionStrategy(),
                    request.getMaxGraphDepth(),
                    request.getMaxRelationsPerEntity()
                );
                graphQueryTime = System.currentTimeMillis() - graphStart;
                log.debug("图谱查询返回 {} 个实体, {} 个关系，耗时: {}ms", 
                    extractionResult.getGraphNodes().size(), 
                    extractionResult.getGraphRelationships().size(),
                    graphQueryTime);
            }

            // 3. 融合向量搜索和图谱查询结果
            long fusionStart = System.currentTimeMillis();
            List<KgEnhancedRagResponse.EnhancedResult> enhancedResults = hybridSearchStrategy.fuseResults(
                vectorResults, 
                extractionResult != null ? extractionResult.getGraphNodes() : new ArrayList<>(),
                extractionResult != null ? extractionResult.getGraphRelationships() : new ArrayList<>(), 
                request);
            fusionTime = System.currentTimeMillis() - fusionStart;

            // 4. 重排序和过滤
            long rerankStart = System.currentTimeMillis();
            List<KgEnhancedRagResponse.EnhancedResult> finalResults = rerankAndFilter(
                enhancedResults, request);
            rerankTime = System.currentTimeMillis() - rerankStart;

            // 5. 构建响应和统计信息
            KgEnhancedRagResponse response = new KgEnhancedRagResponse();
            response.setResults(finalResults);
            response.setVectorResultCount(vectorResults.size());
            response.setGraphEntityCount(extractionResult != null ? extractionResult.getGraphNodes().size() : 0);
            response.setGraphRelationshipCount(extractionResult != null ? extractionResult.getGraphRelationships().size() : 0);
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            response.setSuccess(true);

            // 设置详细统计信息
            KgEnhancedRagResponse.SearchStatistics stats = new KgEnhancedRagResponse.SearchStatistics();
            stats.setTotalQueryTime(response.getProcessingTimeMs());
            stats.setVectorSearchTime(vectorSearchTime);
            stats.setGraphQueryTime(graphQueryTime);
            stats.setFusionTime(fusionTime);
            stats.setRerankTime(rerankTime);
            stats.setExtractedEntitiesCount(extractionResult != null ? extractionResult.getExtractedEntities().size() : 0);
            stats.setGraphQueryCount(extractionResult != null ? extractionResult.getQueryCount() : 0);
            response.setStatistics(stats);

            log.info("知识图谱增强RAG检索完成，最终返回 {} 个结果，处理时间: {}ms (向量:{}ms, 图谱:{}ms, 融合:{}ms, 重排:{}ms)", 
                finalResults.size(), response.getProcessingTimeMs(), vectorSearchTime, graphQueryTime, fusionTime, rerankTime);

            return response;

        } catch (Exception e) {
            log.error("知识图谱增强RAG检索失败", e);
            throw new BusinessException("增强RAG检索失败: " + e.getMessage(), e);
        }
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
            double minThreshold = request.getMinScore() != null ? request.getMinScore() : 0.5;
            results = results.stream()
                .filter(result -> result.getRelevanceScore() >= minThreshold)
                .collect(Collectors.toList());
            
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
}
