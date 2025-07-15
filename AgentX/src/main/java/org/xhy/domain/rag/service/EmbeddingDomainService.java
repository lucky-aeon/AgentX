package org.xhy.domain.rag.service;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

import dev.langchain4j.store.embedding.EmbeddingMatch;
import java.util.ArrayList;
import java.util.List;

import org.dromara.streamquery.stream.core.stream.Steam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.xhy.domain.rag.message.RagDocSyncStorageMessage;
import org.xhy.domain.rag.constant.EmbeddingStatus;
import org.xhy.domain.rag.constant.FileInitializeStatus;
import org.xhy.domain.rag.constant.MetadataConstant;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.repository.FileDetailRepository;
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

    private final OpenAiEmbeddingModel openAiEmbeddingModel;

    private final ApplicationContext applicationContext;

    private final EmbeddingStore<TextSegment> embeddingStore;

    private final FileDetailRepository fileDetailRepository;

    private final DocumentUnitRepository documentUnitRepository;

    private final RerankDomainService rerankService;

    public EmbeddingDomainService(OpenAiEmbeddingModel openAiEmbeddingModel, EmbeddingStore<TextSegment> embeddingStore,
            FileDetailRepository fileDetailRepository, ApplicationContext applicationContext,
            DocumentUnitRepository documentUnitRepository, RerankDomainService rerankService) {
        this.openAiEmbeddingModel = openAiEmbeddingModel;
        this.embeddingStore = embeddingStore;
        this.fileDetailRepository = fileDetailRepository;
        this.applicationContext = applicationContext;
        this.documentUnitRepository = documentUnitRepository;
        this.rerankService = rerankService;
    }

    /** RAG文档检索（支持高级参数和缓存优化）
     * 
     * @param dataSetId 知识库ids
     * @param question 查询问题
     * @param maxResults 最大返回结果数量
     * @param minScore 最小相似度阈值
     * @param enableRerank 是否启用重排序
     * @param candidateMultiplier 候选结果倍数
     * @return 相关文档列表 */
    public List<DocumentUnitEntity> ragDoc(List<String> dataSetId, String question, Integer maxResults, Double minScore,
            Boolean enableRerank, Integer candidateMultiplier) {
        // 参数验证和日志
        if (dataSetId == null || dataSetId.isEmpty()) {
            log.warn("Dataset IDs list is empty");
            return new ArrayList<>();
        }

        if (!StringUtils.hasText(question)) {
            log.warn("Query question is empty");
            return new ArrayList<>();
        }

        // 设置默认值和合理上限
        int finalMaxResults = maxResults != null ? Math.min(maxResults, 100) : 15;
        double finalMinScore = minScore != null ? Math.max(0.0, Math.min(minScore, 1.0)) : 0.7;
        boolean finalEnableRerank = enableRerank != null ? enableRerank : true;
        int finalCandidateMultiplier = candidateMultiplier != null ? Math.max(1, Math.min(candidateMultiplier, 5)) : 2;

        // 记录搜索开始时间
        long startTime = System.currentTimeMillis();

        try {
            // 向量搜索 - 根据是否启用重排序决定搜索数量
            int searchLimit = finalEnableRerank
                    ? Math.max(finalMaxResults * finalCandidateMultiplier, 30)
                    : finalMaxResults;

            log.debug(
                    "Starting RAG search with params: datasets={}, question='{}', maxResults={}, minScore={}, rerank={}, searchLimit={}",
                    dataSetId, question, finalMaxResults, finalMinScore, finalEnableRerank, searchLimit);

            // 向量查询
            final EmbeddingSearchResult<TextSegment> textSegmentList = embeddingStore.search(EmbeddingSearchRequest
                    .builder().filter(new IsIn(DATA_SET_ID, dataSetId)).maxResults(searchLimit).minScore(finalMinScore) // 使用可配置的相似度阈值
                    .queryEmbedding(Embedding.from(openAiEmbeddingModel.embed(question).content().vector())).build());

            List<EmbeddingMatch<TextSegment>> embeddingMatches;

            // 根据配置决定是否进行重排序
            if (finalEnableRerank && !textSegmentList.matches().isEmpty()) {
                long rerankStartTime = System.currentTimeMillis();
                embeddingMatches = rerankService.rerankDocument(textSegmentList, question);
                long rerankTime = System.currentTimeMillis() - rerankStartTime;
                log.debug("Applied reranking for query: '{}', got {} matches, took {}ms", question,
                        embeddingMatches.size(), rerankTime);
            } else {
                embeddingMatches = textSegmentList.matches();
                log.debug("Skipped reranking for query: '{}', using {} vector matches", question,
                        embeddingMatches.size());
            }

            // 如果没有找到相关文档，尝试降低相似度阈值再次搜索
            if (embeddingMatches.isEmpty() && finalMinScore > 0.3) {
                log.info("No results found with minScore: {}, retrying with lower threshold", finalMinScore);

                final EmbeddingSearchResult<TextSegment> fallbackResult = embeddingStore.search(EmbeddingSearchRequest
                        .builder().filter(new IsIn(DATA_SET_ID, dataSetId)).maxResults(searchLimit).minScore(0.3) // 降低阈值进行回退搜索
                        .queryEmbedding(Embedding.from(openAiEmbeddingModel.embed(question).content().vector()))
                        .build());

                embeddingMatches = fallbackResult.matches();
                log.debug("Fallback search found {} matches with lower threshold", embeddingMatches.size());
            }

            // 提取文档ID - 限制最终返回数量并记录相关性分数
            final List<String> documentIds = embeddingMatches.stream().limit(finalMaxResults) // 在重排序后限制数量
                    .map(match -> {
                        if (match.embedded().metadata().containsKey(DOCUMENT_ID)) {
                            String documentId = match.embedded().metadata().getString(DOCUMENT_ID);
                            log.debug("Found document: {} with score: {:.4f}", documentId, match.score());
                            return documentId;
                        }
                        return null;
                    }).filter(StrUtil::isNotBlank).toList();

            if (documentIds.isEmpty()) {
                log.info("No relevant documents found for query: '{}' with minScore: {}", question, finalMinScore);
                return new ArrayList<>();
            }

            // 批量查询文档实体并保持相关性排序
            List<DocumentUnitEntity> documents = documentUnitRepository.selectList(
                    Wrappers.lambdaQuery(DocumentUnitEntity.class).in(DocumentUnitEntity::getId, documentIds));

            // 按照检索相关性顺序重新排列结果
            List<DocumentUnitEntity> sortedResults = documentIds.stream()
                    .map(id -> documents.stream().filter(doc -> id.equals(doc.getId())).findFirst().orElse(null))
                    .filter(java.util.Objects::nonNull).toList();

            // 记录搜索性能统计
            long totalTime = System.currentTimeMillis() - startTime;
            double avgScore = embeddingMatches.stream().mapToDouble(EmbeddingMatch::score).average().orElse(0.0);

            log.info("RAG search completed for query: '{}', returned {} documents, avgScore: {:.4f}, totalTime: {}ms",
                    question, sortedResults.size(), avgScore, totalTime);

            return sortedResults;

        } catch (Exception e) {
            log.error("Error during RAG document retrieval for question: '{}', time: {}ms", question,
                    System.currentTimeMillis() - startTime, e);
            return new ArrayList<>();
        }
    }

    /** RAG文档检索（兼容旧版本接口） */
    public List<DocumentUnitEntity> ragDoc(List<String> dataSetId, String question, Integer maxResults) {
        return ragDoc(dataSetId, question, maxResults, null, null, null);
    }

    /** 批量删除向量数据
     *
     * @param fileIds 文件id集合 */
    public void deleteEmbedding(List<String> fileIds) {

        embeddingStore.removeAll(metadataKey(MetadataConstant.FILE_ID).isIn(fileIds));
    }

    /** 重新向量入库
     *
     * @param fileId 文件ID
     * @return 处理结果，true表示成功，false表示失败 */
    @Transactional(rollbackFor = Exception.class)
    public boolean reindexEmbedding(String fileId) {

        if (!StringUtils.hasText(fileId)) {
            log.warn("File ID for re-vectorization is empty");
            return false;
        }

        try {
            // 获取文件详情
            FileDetailEntity fileDetail = fileDetailRepository.selectById(fileId);
            if (fileDetail == null) {
                log.warn("File with ID {}not found", fileId);
                return false;
            }

            if (ObjectUtil.notEqual(fileDetail.getIsInitialize(), FileInitializeStatus.INITIALIZED)) {
                log.warn("The file with ID {} has no data", fileId);
            }

            // 更新文件状态为入库中
            fileDetail.setIsEmbedding(EmbeddingStatus.INITIALIZING);
            fileDetailRepository.updateById(fileDetail);

            // 删除旧向量数据
            removeEmbeddingByFileId(fileId);

            // Todo 获取当前文件的所有文档id
            // Todo 设置文档表为未向量化
            final List<DocumentUnitEntity> documentUnitEntities = new ArrayList<>();

            indexEmbedding(documentUnitEntities);

            return true;
        } catch (Exception e) {
            log.error("File {} re-vectorization and storage failed", fileId, e);
            // 更新文件状态为入库失败
            try {
                FileDetailEntity fileDetail = fileDetailRepository.selectById(fileId);
                if (fileDetail != null) {
                    fileDetail.setIsEmbedding(EmbeddingStatus.INITIALIZATION_FAILED);
                    fileDetailRepository.updateById(fileDetail);
                }
            } catch (Exception ex) {
                log.error("Failed to update file status", ex);
            }
            return false;
        }
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

        final String docId = ragDocSyncStorageMessage.getId();

        final DocumentUnitEntity documentUnitEntity = documentUnitRepository.selectById(docId);

        final FileDetailEntity fileDetailEntity = fileDetailRepository.selectById(ragDocSyncStorageMessage.getFileId());
        if (documentUnitEntity == null) {
            return;
        }

        final String content = documentUnitEntity.getContent();

        final Metadata documentMetadata = buildMetadata(ragDocSyncStorageMessage);

        final TextSegment textSegment = new TextSegment(content, documentMetadata);

        // 生成嵌入向量并保存TextSegment（包含content和metadata）
        Embedding embeddings = openAiEmbeddingModel.embed(textSegment).content();

        embeddingStore.add(embeddings, textSegment);

        documentUnitRepository.update(Wrappers.lambdaUpdate(DocumentUnitEntity.class)
                .eq(DocumentUnitEntity::getId, docId).set(DocumentUnitEntity::getIsVector, true));

        // 修改文件状态
        final Integer pageSize = fileDetailEntity.getFilePageSize();

        final Long isVector = documentUnitRepository.selectCount(Wrappers.lambdaQuery(DocumentUnitEntity.class)
                .eq(DocumentUnitEntity::getFileId, documentUnitEntity.getFileId())
                .eq(DocumentUnitEntity::getIsVector, true));

        final Integer anInt = Convert.toInt(isVector);

        if (anInt >= pageSize) {
            fileDetailRepository.update(
                    Wrappers.lambdaUpdate(FileDetailEntity.class).eq(FileDetailEntity::getId, fileDetailEntity.getId())
                            .set(FileDetailEntity::getIsEmbedding, EmbeddingStatus.INITIALIZED));
        }

    }

    private Metadata buildMetadata(RagDocSyncStorageMessage ragDocSyncStorageMessage) {

        final Metadata metadata = new Metadata();
        metadata.put(FILE_ID, ragDocSyncStorageMessage.getFileId());
        metadata.put(FILE_NAME, ragDocSyncStorageMessage.getFileName());
        metadata.put(DOCUMENT_ID, ragDocSyncStorageMessage.getId());
        metadata.put(DATA_SET_ID, ragDocSyncStorageMessage.getDatasetId());
        return metadata;
    }
}
