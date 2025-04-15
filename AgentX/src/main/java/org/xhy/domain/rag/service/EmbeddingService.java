package org.xhy.domain.rag.service;

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
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;

/**
 * 向量话存储
 * @author shilong.zang
 * @date 18:28 <br/>
 */
@Component
public class EmbeddingService implements MetadataConstant {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final OpenAiEmbeddingModel openAiEmbeddingModel;

    private final ApplicationContext applicationContext;

    private final EmbeddingStore<TextSegment> embeddingStore;
    
    private final FileDetailRepository fileDetailRepository;

    private final DocumentUnitRepository documentUnitRepository;

    public EmbeddingService(OpenAiEmbeddingModel openAiEmbeddingModel, 
                           EmbeddingStore<TextSegment> embeddingStore,
                           FileDetailRepository fileDetailRepository,
                            ApplicationContext applicationContext,
                            DocumentUnitRepository documentUnitRepository) {
        this.openAiEmbeddingModel = openAiEmbeddingModel;
        this.embeddingStore = embeddingStore;
        this.fileDetailRepository = fileDetailRepository;
        this.applicationContext = applicationContext;
        this.documentUnitRepository = documentUnitRepository;
    }

    /**
     * 批量删除向量数据
     * @param embeddingIds 向量数据id
     */
    public void deleteEmbedding(List<String> embeddingIds) {
        embeddingStore.removeAll(embeddingIds);
    }

    /**
     * 重新向量入库
     * @param fileId 文件ID
     * @return 处理结果，true表示成功，false表示失败
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean reindexEmbedding(String fileId) {
        if (!StringUtils.hasText(fileId)) {
            log.warn("重新向量入库的文件ID为空");
            return false;
        }

        try {
            // 获取文件详情
            FileDetailEntity fileDetail = fileDetailRepository.selectById(fileId);
            if (fileDetail == null) {
                log.warn("未找到ID为{}的文件", fileId);
                return false;
            }

            if (ObjectUtil.notEqual(fileDetail.getIsInitialize(), FileInitializeStatus.INITIALIZED)) {
                log.warn("ID为{}的文件没有任何数据",fileId);
            }

            // 更新文件状态为入库中
            fileDetail.setIsEmbedding(EmbeddingStatus.INITIALIZING);
            fileDetailRepository.updateById(fileDetail);

            // 删除旧向量数据
            removeEmbeddingByFileId(fileId);

            // Todo 获取当前文件的所有文档id
            //Todo 设置文档表为未向量化
            final List<DocumentUnitEntity> documentUnitEntities = new ArrayList<>();

            indexEmbedding(documentUnitEntities);

            return true;
        } catch (Exception e) {
            log.error("文件{}重新向量入库失败", fileId, e);
            // 更新文件状态为入库失败
            try {
                FileDetailEntity fileDetail = fileDetailRepository.selectById(fileId);
                if (fileDetail != null) {
                    fileDetail.setIsEmbedding(EmbeddingStatus.INITIALIZATION_FAILED);
                    fileDetailRepository.updateById(fileDetail);
                }
            } catch (Exception ex) {
                log.error("更新文件状态失败", ex);
            }
            return false;
        }
    }
    
    /**
     * 获取与文件关联的向量ID列表
     * @param fileId 文件ID
     */
    private void removeEmbeddingByFileId(String fileId) {

        embeddingStore.removeAll(new IsEqualTo(FILE_ID, fileId));
    }

    /**
     * 批量向量化入库
     */
    private void indexEmbedding(List<DocumentUnitEntity> documentUnitEntityList) {

        Steam.of(documentUnitEntityList).forEach(documentUnit -> applicationContext.publishEvent(new RagDocSyncStorageEvent<>(documentUnit, EventType.DOC_SYNC_RAG)));

    }

    /**
     * 文本向量化
     */
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

        // 生成嵌入向量
        Embedding embeddings = openAiEmbeddingModel.embed(textSegment).content();

        embeddingStore.add(embeddings);

        documentUnitRepository.update(Wrappers.lambdaUpdate(DocumentUnitEntity.class).eq(DocumentUnitEntity::getId, docId).set(DocumentUnitEntity::getVector,true));

        // 修改文件状态
        final Integer pageSize = fileDetailEntity.getFilePageSize();

        final Long isVector = documentUnitRepository.selectCount(Wrappers.lambdaQuery(DocumentUnitEntity.class).eq(DocumentUnitEntity::getFileId, documentUnitEntity.getFileId()).eq(DocumentUnitEntity::getVector, true));

        final Integer anInt = Convert.toInt(isVector);

        if (anInt>=pageSize) {
            fileDetailRepository.update(Wrappers.lambdaUpdate(FileDetailEntity.class).eq(FileDetailEntity::getId, fileDetailEntity.getId()).set(FileDetailEntity::getIsEmbedding, EmbeddingStatus.INITIALIZED));
        }

    }

    private Metadata buildMetadata(RagDocSyncStorageMessage ragDocSyncStorageMessage) {

        final Metadata metadata = new Metadata();
        metadata.put(FILE_ID, ragDocSyncStorageMessage.getFileId());
        metadata.put(FILE_NAME, ragDocSyncStorageMessage.getFileName());
        metadata.put(DOCUMENT_ID, ragDocSyncStorageMessage.getId());
        return metadata;
    }
}
