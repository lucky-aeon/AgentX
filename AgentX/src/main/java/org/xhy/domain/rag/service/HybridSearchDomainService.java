package org.xhy.domain.rag.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.constant.SearchType;
import org.xhy.domain.rag.dto.HybridSearchConfig;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.model.VectorStoreResult;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.infrastructure.rag.factory.EmbeddingModelFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** 混合检索领域服务 协调向量检索和关键词检索，实现RRF融合算法
 * 
 * @author claude */
@Service
public class HybridSearchDomainService {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchDomainService.class);

    /** RRF算法参数k，用于平衡不同检索方式的排序差异 */
    private static final int RRF_K = 60;

    /** 并行检索的超时时间（秒） */
    private static final int SEARCH_TIMEOUT_SECONDS = 30;

    private final EmbeddingDomainService embeddingDomainService;
    private final KeywordSearchDomainService keywordSearchDomainService;
    private final DocumentUnitRepository documentUnitRepository;
    private final RerankDomainService rerankDomainService;
    private final HyDEDomainService hydeDomainService;

    public HybridSearchDomainService(EmbeddingDomainService embeddingDomainService,
            KeywordSearchDomainService keywordSearchDomainService, DocumentUnitRepository documentUnitRepository,
            RerankDomainService rerankDomainService, HyDEDomainService hydeDomainService) {
        this.embeddingDomainService = embeddingDomainService;
        this.keywordSearchDomainService = keywordSearchDomainService;
        this.documentUnitRepository = documentUnitRepository;
        this.rerankDomainService = rerankDomainService;
        this.hydeDomainService = hydeDomainService;
    }

    /** 执行混合检索 并行执行向量检索和关键词检索，使用RRF算法融合结果
     * 
     * @param config 混合检索配置对象
     * @return 混合检索结果列表 */
    public List<DocumentUnitEntity> hybridSearch(HybridSearchConfig config) {

        // 参数验证
        if (config == null) {
            log.warn("混合搜索配置为空");
            return Collections.emptyList();
        }

        if (!config.isValid()) {
            String error = config.getValidationError();
            log.warn("无效的混合搜索配置: {}", error);
            return Collections.emptyList();
        }

        // 设置默认值
        int finalMaxResults = config.getMaxResults() != null ? Math.min(config.getMaxResults(), 100) : 15;
        Double finalMinScore = config.getMinScore() != null ? Math.max(0.0, Math.min(config.getMinScore(), 1.0)) : 0.7;

        long startTime = System.currentTimeMillis();

        try {
            log.info("开始混合搜索 查询: '{}', 数据集: {}, 最大结果数: {}, HyDE可用: {}", config.getQuestion(),
                    config.getDataSetIds().size(), finalMaxResults, config.hasValidChatModelConfig());

            // HyDE处理：生成假设文档用于向量检索
            String hypotheticalDocument = hydeDomainService.generateHypotheticalDocument(config.getQuestion(),
                    config.getChatModelConfig());
            config.setQuestion(hypotheticalDocument);

            CompletableFuture<List<VectorStoreResult>> vectorSearchFuture = CompletableFuture
                    .supplyAsync(() -> embeddingDomainService.vectorSearch(config.getDataSetIds(), config.getQuestion(),
                            finalMaxResults * 2, finalMinScore, false, config.getCandidateMultiplier(),
                            config.getEmbeddingConfig()));

            CompletableFuture<List<VectorStoreResult>> keywordSearchFuture = CompletableFuture
                    .supplyAsync(() -> keywordSearchDomainService.keywordSearch(config.getDataSetIds(),
                            config.getQuestion(), finalMaxResults * 2));

            // 等待两个检索任务完成
            List<VectorStoreResult> vectorResults = Collections.emptyList();
            List<VectorStoreResult> keywordResults = Collections.emptyList();

            try {
                vectorResults = vectorSearchFuture.get(SEARCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                log.debug("向量搜索完成，找到{}个结果", vectorResults.size());
            } catch (Exception e) {
                log.warn("向量搜索失败或超时: {}", e.getMessage());
            }

            try {
                keywordResults = keywordSearchFuture.get(SEARCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                log.debug("关键词搜索完成，找到{}个结果", keywordResults.size());
            } catch (Exception e) {
                log.warn("关键词搜索失败或超时: {}", e.getMessage());
            }

            // 如果两个检索都失败，返回空结果
            if (vectorResults.isEmpty() && keywordResults.isEmpty()) {
                log.warn("向量和关键词搜索对于查询'{}'都返回空结果", config.getQuestion());
                return Collections.emptyList();
            }

            // 使用RRF算法融合结果
            List<VectorStoreResult> fusedResults = fusionWithRRF(vectorResults, keywordResults, finalMaxResults);

            // RRF融合后进行重排序（如果启用）
            List<VectorStoreResult> rerankedResults = fusedResults;
            if (Boolean.TRUE.equals(config.getEnableRerank()) && !fusedResults.isEmpty()) {
                rerankedResults = applyRerankToFusedResults(fusedResults, config.getQuestion());
            }

            return convertToDocumentUnits(rerankedResults, config.getEnableQueryExpansion());

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("混合搜索过程中出现错误，查询: '{}', 耗时: {}ms", config.getQuestion(), totalTime, e);
            return Collections.emptyList();
        }
    }

    /** 执行混合检索（重载方法，保持向后兼容）
     * @deprecated 推荐使用 hybridSearch(HybridSearchConfig config) 方法 */
    @Deprecated
    public List<DocumentUnitEntity> hybridSearch(List<String> dataSetIds, String question, Integer maxResults,
            Double minScore, Boolean enableRerank, Integer candidateMultiplier,
            EmbeddingModelFactory.EmbeddingConfig embeddingConfig, Boolean enableQueryExpansion) {

        log.warn("使用已废弃的hybridSearch重载方法，建议使用HybridSearchConfig配置对象");

        // 转换为新的配置对象
        HybridSearchConfig config = HybridSearchConfig.builder(dataSetIds, question).maxResults(maxResults)
                .minScore(minScore).enableRerank(enableRerank).candidateMultiplier(candidateMultiplier)
                .embeddingConfig(embeddingConfig).enableQueryExpansion(enableQueryExpansion).build();

        return hybridSearch(config);
    }

    /** 使用RRF算法融合向量检索和关键词检索结果 RRF公式：RRF(d) = Σ(1/(k + rank_i(d)))，其中k=60
     * 
     * @param vectorResults 向量检索结果
     * @param keywordResults 关键词检索结果
     * @param maxResults 最大返回结果数量
     * @return 融合后的结果列表 */
    private List<VectorStoreResult> fusionWithRRF(List<VectorStoreResult> vectorResults,
            List<VectorStoreResult> keywordResults, int maxResults) {

        log.debug("开始RRF融合 向量: {}, 关键词: {} 结果", vectorResults.size(), keywordResults.size());

        // 存储每个文档的RRF分数
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, VectorStoreResult> documentMap = new HashMap<>();

        // 处理向量检索结果
        for (int i = 0; i < vectorResults.size(); i++) {
            VectorStoreResult result = vectorResults.get(i);
            String documentId = result.getDocumentId();

            if (documentId != null && !documentId.trim().isEmpty()) {
                double rrfScore = 1.0 / (RRF_K + i + 1); // rank从1开始
                rrfScores.put(documentId, rrfScores.getOrDefault(documentId, 0.0) + rrfScore);

                // 保存文档信息（优先保留向量检索的结果）
                if (!documentMap.containsKey(documentId)) {
                    result.setSearchType(SearchType.HYBRID);
                    documentMap.put(documentId, result);
                }

                log.debug("Vector result {}: docId={}, originalScore={}, rrfContribution={}", i + 1, documentId,
                        result.getScore(), rrfScore);
            }
        }

        // 处理关键词检索结果
        for (int i = 0; i < keywordResults.size(); i++) {
            VectorStoreResult result = keywordResults.get(i);
            String documentId = result.getDocumentId();

            if (documentId != null && !documentId.trim().isEmpty()) {
                double rrfScore = 1.0 / (RRF_K + i + 1); // rank从1开始
                rrfScores.put(documentId, rrfScores.getOrDefault(documentId, 0.0) + rrfScore);

                // 如果向量检索中没有此文档，保存关键词检索的结果
                if (!documentMap.containsKey(documentId)) {
                    result.setSearchType(SearchType.HYBRID);
                    documentMap.put(documentId, result);
                }

                log.debug("Keyword result {}: docId={}, originalScore={}, rrfContribution={}", i + 1, documentId,
                        result.getScore(), rrfScore);
            }
        }

        // 按RRF分数排序并返回
        List<VectorStoreResult> fusedResults = rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(maxResults).map(entry -> {
                    String documentId = entry.getKey();
                    Double rrfScore = entry.getValue();
                    VectorStoreResult result = documentMap.get(documentId);

                    // 设置融合后的分数
                    result.setScore(rrfScore);

                    log.debug("Fused result: docId={}, finalRRFScore={}", documentId, rrfScore);
                    return result;
                }).collect(Collectors.toList());

        log.info("RRF融合完成: {}个唯一文档，选择前{}个", documentMap.size(), fusedResults.size());

        return fusedResults;
    }

    /** 将VectorStoreResult转换为DocumentUnitEntity 包括查询扩展逻辑
     * 
     * @param vectorStoreResults 向量存储检索结果
     * @param enableQueryExpansion 是否启用查询扩展
     * @return DocumentUnitEntity列表 */
    private List<DocumentUnitEntity> convertToDocumentUnits(List<VectorStoreResult> vectorStoreResults,
            Boolean enableQueryExpansion) {

        if (vectorStoreResults.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取文档ID
        List<String> documentIds = vectorStoreResults.stream().map(VectorStoreResult::getDocumentId)
                .filter(id -> id != null && !id.trim().isEmpty()).collect(Collectors.toList());

        if (documentIds.isEmpty()) {
            log.warn("在向量存储结果中未找到有效的文档ID");
            return Collections.emptyList();
        }

        // 查询文档实体
        List<DocumentUnitEntity> documents = documentUnitRepository
                .selectList(Wrappers.lambdaQuery(DocumentUnitEntity.class).in(DocumentUnitEntity::getId, documentIds));

        if (documents.isEmpty()) {
            log.warn("未找到文档ID对应的DocumentUnitEntity: {}", documentIds);
            return Collections.emptyList();
        }

        // 创建分数映射
        Map<String, Double> scoreMap = vectorStoreResults.stream().collect(Collectors.toMap(
                VectorStoreResult::getDocumentId, VectorStoreResult::getScore, (existing, replacement) -> existing // 保留第一个值，避免重复key
        ));

        // 设置相似度分数
        documents.forEach(doc -> {
            Double score = scoreMap.get(doc.getId());
            if (score != null) {
                doc.setSimilarityScore(score);
            }
        });

        // 查询扩展处理
        if (Boolean.TRUE.equals(enableQueryExpansion)) {
            documents = expandQueryResults(documents, scoreMap);
        }

        // 按照原始RRF分数排序返回
        documents.sort((a, b) -> {
            Double scoreA = a.getSimilarityScore();
            Double scoreB = b.getSimilarityScore();
            if (scoreA == null)
                scoreA = 0.0;
            if (scoreB == null)
                scoreB = 0.0;
            return Double.compare(scoreB, scoreA); // 降序排序
        });

        log.debug("转换{}个VectorStoreResult为{}个DocumentUnitEntity", vectorStoreResults.size(), documents.size());

        return documents;
    }

    /** 查询扩展：添加相邻页面的文档片段
     * 
     * @param documents 原始文档列表
     * @param scoreMap 分数映射
     * @return 扩展后的文档列表 */
    private List<DocumentUnitEntity> expandQueryResults(List<DocumentUnitEntity> documents,
            Map<String, Double> scoreMap) {

        Set<String> expandedIds = new LinkedHashSet<>();
        List<DocumentUnitEntity> expandedDocuments = new ArrayList<>(documents);

        // 为原始结果添加ID
        documents.forEach(doc -> expandedIds.add(doc.getId()));

        for (DocumentUnitEntity doc : documents) {
            try {
                // 查询相邻页面片段（前一页、当前页、后一页）
                List<DocumentUnitEntity> adjacentChunks = documentUnitRepository.selectList(
                        Wrappers.<DocumentUnitEntity>lambdaQuery().eq(DocumentUnitEntity::getFileId, doc.getFileId())
                                .between(DocumentUnitEntity::getPage, Math.max(1, doc.getPage() - 1), doc.getPage() + 1)
                                .eq(DocumentUnitEntity::getIsVector, true));

                for (DocumentUnitEntity chunk : adjacentChunks) {
                    if (!expandedIds.contains(chunk.getId())) {
                        // 为扩展片段设置较低的分数
                        Double originalScore = scoreMap.get(doc.getId());
                        if (originalScore != null) {
                            chunk.setSimilarityScore(originalScore * 0.8);
                        } else {
                            chunk.setSimilarityScore(0.5);
                        }

                        expandedDocuments.add(chunk);
                        expandedIds.add(chunk.getId());
                    }
                }
            } catch (Exception e) {
                log.warn("为文档{}扩展查询失败", doc.getId(), e);
            }
        }

        log.info("查询扩展: {}个原始文档扩展为{}个总文档", documents.size(), expandedDocuments.size());

        return expandedDocuments;
    }

    /** 对RRF融合后的结果进行重排序
     * 
     * @param fusedResults RRF融合后的结果
     * @param question 查询问题
     * @return 重排序后的结果 */
    private List<VectorStoreResult> applyRerankToFusedResults(List<VectorStoreResult> fusedResults, String question) {
        if (fusedResults.isEmpty()) {
            return fusedResults;
        }

        long rerankStartTime = System.currentTimeMillis();

        try {
            // 提取文档文本列表
            List<String> texts = fusedResults.stream().map(VectorStoreResult::getText).collect(Collectors.toList());

            // 调用重排序服务获取重排序后的索引
            List<Integer> rerankedIndices = rerankDomainService.rerank(texts, question);

            // 根据重排序索引重新排列结果
            List<VectorStoreResult> rerankedResults = rerankedIndices.stream()
                    .filter(index -> index >= 0 && index < fusedResults.size()) // 确保索引有效
                    .map(fusedResults::get).collect(Collectors.toList());

            long rerankTime = System.currentTimeMillis() - rerankStartTime;
            log.info("对查询'{}'的融合结果应用重排序，{}个结果，耗时{}ms", question, rerankedResults.size(), rerankTime);

            return rerankedResults;

        } catch (Exception e) {
            long rerankTime = System.currentTimeMillis() - rerankStartTime;
            log.error("对查询'{}'的融合结果重排序失败，耗时{}ms", question, rerankTime, e);
            // 重排序失败时返回原始融合结果
            return fusedResults;
        }
    }
}