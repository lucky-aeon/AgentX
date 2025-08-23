package org.xhy.domain.rag.service;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

import dev.langchain4j.store.embedding.EmbeddingMatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.xhy.domain.rag.constant.SearchType;
import org.xhy.domain.rag.model.VectorStoreResult;

import org.dromara.streamquery.stream.core.stream.Steam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.xhy.domain.rag.message.RagDocSyncStorageMessage;
import org.xhy.domain.rag.constant.FileProcessingStatusEnum;
import org.xhy.domain.rag.constant.MetadataConstant;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.repository.FileDetailRepository;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.mq.enums.EventType;
import org.xhy.infrastructure.mq.events.RagDocSyncStorageEvent;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.xhy.infrastructure.rag.factory.EmbeddingModelFactory;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsIn;

/** 向量话存储
 *
 * @author shilong.zang
 * @date 18:28 <br/>
 */
@Component
public class EmbeddingDomainService implements MetadataConstant {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingDomainService.class);

    private final EmbeddingModelFactory embeddingModelFactory;

    private final ApplicationContext applicationContext;

    private final EmbeddingStore<TextSegment> embeddingStore;

    private final FileDetailRepository fileDetailRepository;

    private final DocumentUnitRepository documentUnitRepository;

    public EmbeddingDomainService(EmbeddingModelFactory embeddingModelFactory,
            EmbeddingStore<TextSegment> embeddingStore, FileDetailRepository fileDetailRepository,
            ApplicationContext applicationContext, DocumentUnitRepository documentUnitRepository
          ) {
        this.embeddingModelFactory = embeddingModelFactory;
        this.embeddingStore = embeddingStore;
        this.fileDetailRepository = fileDetailRepository;
        this.applicationContext = applicationContext;
        this.documentUnitRepository = documentUnitRepository;
    }

    /** 纯向量检索方法 专门负责向量检索算法，返回统一的VectorStoreResult格式
     * 
     * @param dataSetIds 数据集ID列表
     * @param question 查询问题
     * @param maxResults 最大返回结果数量
     * @param minScore 最小相似度阈值
     * @param enableRerank 是否启用重排序
     * @param candidateMultiplier 候选结果倍数
     * @param embeddingConfig 嵌入模型配置
     * @return 向量检索结果列表，失败时返回空集合 */
    public List<VectorStoreResult> vectorSearch(List<String> dataSetIds, String question, Integer maxResults,
            Double minScore, Boolean enableRerank, Integer candidateMultiplier,
            EmbeddingModelFactory.EmbeddingConfig embeddingConfig) {
        // 参数验证
        if (dataSetIds == null || dataSetIds.isEmpty()) {
            log.warn("Dataset IDs list is empty for vector search");
            return Collections.emptyList();
        }

        if (!StringUtils.hasText(question)) {
            log.warn("Query question is empty for vector search");
            return Collections.emptyList();
        }

        if (embeddingConfig == null) {
            log.warn("Embedding model config is null for vector search");
            return Collections.emptyList();
        }

        // 设置默认值和合理上限
        int finalMaxResults = maxResults != null ? Math.min(maxResults, 100) : 15;
        double finalMinScore = minScore != null ? Math.max(0.0, Math.min(minScore, 1.0)) : 0.7;
        boolean finalEnableRerank = enableRerank != null ? enableRerank : true;
        int finalCandidateMultiplier = candidateMultiplier != null ? Math.max(1, Math.min(candidateMultiplier, 5)) : 2;

        long startTime = System.currentTimeMillis();

        try {
            // 创建嵌入模型实例
            OpenAiEmbeddingModel embeddingModel = embeddingModelFactory.createEmbeddingModel(embeddingConfig);

            // 计算搜索数量
            int searchLimit = finalEnableRerank
                    ? Math.max(finalMaxResults * finalCandidateMultiplier, 30)
                    : finalMaxResults;

            log.debug(
                    "Starting vector search with params: datasets={}, question='{}', maxResults={}, minScore={}, searchLimit={}",
                    dataSetIds, question, finalMaxResults, finalMinScore, searchLimit);

            // 执行向量查询
            final EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(EmbeddingSearchRequest
                    .builder().filter(new IsIn(DATA_SET_ID, dataSetIds)).maxResults(searchLimit).minScore(finalMinScore)
                    .queryEmbedding(Embedding.from(embeddingModel.embed(question).content().vector())).build());

            List<EmbeddingMatch<TextSegment>> embeddingMatches = searchResult.matches();

            // 回退搜索（降低阈值）
            if (embeddingMatches.isEmpty() && finalMinScore > 0.3) {
                log.info("No vector results found with minScore: {}, retrying with lower threshold", finalMinScore);
                final EmbeddingSearchResult<TextSegment> fallbackResult = embeddingStore.search(EmbeddingSearchRequest
                        .builder().filter(new IsIn(DATA_SET_ID, dataSetIds)).maxResults(searchLimit).minScore(0.3)
                        .queryEmbedding(Embedding.from(embeddingModel.embed(question).content().vector())).build());
                embeddingMatches = fallbackResult.matches();
                log.debug("Fallback vector search found {} matches", embeddingMatches.size());
            }

            // 转换为VectorStoreResult格式
            List<VectorStoreResult> results = embeddingMatches.stream().limit(finalMaxResults).map(match -> {
                VectorStoreResult result = new VectorStoreResult();
                result.setEmbeddingId(match.embeddingId());
                result.setText(match.embedded().text());
                result.setMetadata(match.embedded().metadata().toMap());
                result.setScore(match.score());
                result.setSearchType(SearchType.VECTOR);
                return result;
            }).toList();

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("Vector search completed for query: '{}', returned {} documents, took {}ms", question,
                    results.size(), totalTime);

            return results;

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("Error during vector search for question: '{}', time: {}ms", question, totalTime, e);
            // 向量检索失败时返回空集合，不影响关键词检索结果
            return Collections.emptyList();
        }
    }

    /** 批量删除向量数据
     *
     * @param fileIds 文件id集合 */
    public void deleteEmbedding(List<String> fileIds) {

        embeddingStore.removeAll(metadataKey(MetadataConstant.FILE_ID).isIn(fileIds));
    }

    /** 获取与文件关联的向量ID列表
     *
     * @param fileId 文件ID */
    private void removeEmbeddingByFileId(String fileId) {

        embeddingStore.removeAll(new IsEqualTo(FILE_ID, fileId));
    }

    /** 批量向量化入库 */
    private void indexEmbedding(List<DocumentUnitEntity> documentUnitEntityList) {

        Steam.of(documentUnitEntityList).forEach(documentUnit -> applicationContext
                .publishEvent(new RagDocSyncStorageEvent<>(documentUnit, EventType.DOC_SYNC_RAG)));

    }

    /** 文本向量化 */
    public void syncStorage(RagDocSyncStorageMessage ragDocSyncStorageMessage) {

        final String vectorId = ragDocSyncStorageMessage.getId();
        final FileDetailEntity fileDetailEntity = fileDetailRepository.selectById(ragDocSyncStorageMessage.getFileId());

        // 使用消息中的翻译后内容，而不是从数据库读取原文
        final String content = ragDocSyncStorageMessage.getContent();

        if (content == null || content.trim().isEmpty()) {
            log.warn("Empty content in storage message {}, skipping vectorization", vectorId);
            return;
        }

        final Metadata documentMetadata = buildMetadata(ragDocSyncStorageMessage);

        final TextSegment textSegment = new TextSegment(content, documentMetadata);

        // 使用消息中配置的嵌入模型生成向量
        OpenAiEmbeddingModel embeddingModel = createEmbeddingModelFromMessage(ragDocSyncStorageMessage);
        Embedding embeddings = embeddingModel.embed(textSegment).content();

        embeddingStore.add(embeddings, textSegment);

        // 提取原始DocumentUnit ID（移除segment后缀）
        String originalDocId = extractOriginalDocId(vectorId);

        // 更新原始DocumentUnit的向量化状态
        if (originalDocId != null) {
            documentUnitRepository.update(Wrappers.lambdaUpdate(DocumentUnitEntity.class)
                    .eq(DocumentUnitEntity::getId, originalDocId).set(DocumentUnitEntity::getIsVector, true));
        }

        // 修改文件状态
        final Integer pageSize = fileDetailEntity.getFilePageSize();

        final Long isVector = documentUnitRepository.selectCount(Wrappers.lambdaQuery(DocumentUnitEntity.class)
                .eq(DocumentUnitEntity::getFileId, ragDocSyncStorageMessage.getFileId())
                .eq(DocumentUnitEntity::getIsVector, true));

        final Integer anInt = Convert.toInt(isVector);

        if (anInt >= pageSize) {
            // 使用状态机设置完成状态
            fileDetailRepository.update(
                    Wrappers.lambdaUpdate(FileDetailEntity.class).eq(FileDetailEntity::getId, fileDetailEntity.getId())
                            .set(FileDetailEntity::getProcessingStatus, FileProcessingStatusEnum.COMPLETED.getCode()));
        }

    }

    /** 从向量ID中提取原始DocumentUnit ID */
    private String extractOriginalDocId(String vectorId) {
        if (vectorId == null) {
            return null;
        }

        // 如果ID包含segment后缀，则提取原始ID
        if (vectorId.contains("_segment_")) {
            return vectorId.substring(0, vectorId.indexOf("_segment_"));
        }

        // 否则直接返回（兼容旧格式）
        return vectorId;
    }

    private Metadata buildMetadata(RagDocSyncStorageMessage ragDocSyncStorageMessage) {

        final Metadata metadata = new Metadata();
        metadata.put(FILE_ID, ragDocSyncStorageMessage.getFileId());
        metadata.put(FILE_NAME, ragDocSyncStorageMessage.getFileName());
        metadata.put(DOCUMENT_ID, extractOriginalDocId(ragDocSyncStorageMessage.getId()));
        metadata.put(DATA_SET_ID, ragDocSyncStorageMessage.getDatasetId());
        return metadata;
    }

    /** 从消息中创建嵌入模型
     * 
     * @param ragDocSyncStorageMessage 存储消息
     * @return OpenAiEmbeddingModel实例
     * @throws RuntimeException 如果没有配置嵌入模型或创建失败 */
    private OpenAiEmbeddingModel createEmbeddingModelFromMessage(RagDocSyncStorageMessage ragDocSyncStorageMessage) {
        // 检查消息和模型配置是否存在
        if (ragDocSyncStorageMessage == null || ragDocSyncStorageMessage.getEmbeddingModelConfig() == null) {
            String errorMsg = String.format("用户 %s 未配置嵌入模型，无法进行向量化处理",
                    ragDocSyncStorageMessage != null ? ragDocSyncStorageMessage.getUserId() : "unknown");
            log.error(errorMsg);
            throw new BusinessException(errorMsg);
        }

        try {
            var modelConfig = ragDocSyncStorageMessage.getEmbeddingModelConfig();

            // 验证模型配置的完整性
            if (modelConfig.getModelId() == null || modelConfig.getApiKey() == null
                    || modelConfig.getBaseUrl() == null) {
                String errorMsg = String.format("用户 %s 的嵌入模型配置不完整: modelId=%s, apiKey=%s, baseUrl=%s",
                        ragDocSyncStorageMessage.getUserId(), modelConfig.getModelId(),
                        modelConfig.getApiKey() != null ? "已配置" : "未配置", modelConfig.getBaseUrl());
                log.error(errorMsg);
                throw new BusinessException(errorMsg);
            }

            // 使用工厂类创建嵌入模型
            EmbeddingModelFactory.EmbeddingConfig config = new EmbeddingModelFactory.EmbeddingConfig(
                    modelConfig.getApiKey(), modelConfig.getBaseUrl(), modelConfig.getModelId());
            OpenAiEmbeddingModel embeddingModel = embeddingModelFactory.createEmbeddingModel(config);

            log.info("Successfully created embedding model for user {}: {}", ragDocSyncStorageMessage.getUserId(),
                    modelConfig.getModelId());
            return embeddingModel;

        } catch (RuntimeException e) {
            // 重新抛出已知的业务异常
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("用户 %s 创建嵌入模型失败: %s", ragDocSyncStorageMessage.getUserId(), e.getMessage());
            log.error(errorMsg, e);
            throw new BusinessException(errorMsg, e);
        }
    }
}
