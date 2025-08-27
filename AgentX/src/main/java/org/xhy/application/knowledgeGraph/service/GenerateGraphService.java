package org.xhy.application.knowledgeGraph.service;

import java.util.List;
import org.dromara.streamquery.stream.core.stream.Steam;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.xhy.domain.knowledgeGraph.message.DocIeInferMessage;
import org.xhy.domain.knowledgeGraph.service.KnowledgeGraphIeService;
import org.xhy.domain.rag.message.RagDocSyncOcrMessage;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.service.DocumentUnitDomainService;
import org.xhy.infrastructure.mq.enums.EventType;
import org.xhy.infrastructure.mq.events.DocIeInferEvent;
import org.xhy.infrastructure.mq.events.RagDocSyncOcrEvent;

/**
 * 图谱生成服务
 * @author shilong.zang
 * @date 14:33 <br/>
 */
@Service
public class GenerateGraphService {

    private final DocumentUnitDomainService documentUnitDomainService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public GenerateGraphService(DocumentUnitDomainService documentUnitDomainService,
            ApplicationEventPublisher applicationEventPublisher) {
        this.documentUnitDomainService = documentUnitDomainService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * 生成图谱（分页处理模式）
     * @param fileId 文件ID
     * @return 图谱生成结果描述
     */
    public String generateGraph(String fileId) {

        final List<DocumentUnitEntity> documentUnitEntities = documentUnitDomainService.listDocumentUnitsByFileId(
                fileId);

        if (documentUnitEntities == null || documentUnitEntities.isEmpty()) {
            throw new IllegalArgumentException("No document units found for fileId: " + fileId);
        }

        // 获取文档总页数
        int totalPages = documentUnitEntities.size();
        
        // 逐页发送消息进行知识图谱提取
        for (int i = 0; i < totalPages; i++) {
            DocumentUnitEntity documentUnit = documentUnitEntities.get(i);
            Integer pageNumber = documentUnit.getPage() != null ? documentUnit.getPage() : (i + 1);
            
            // 创建分页消息
            final DocIeInferMessage docIeInferMessage = new DocIeInferMessage(
                    fileId,                          // 文件ID
                    null,                            // 文件名（可选）
                    documentUnit.getContent(),       // 当前页内容
                    pageNumber+1,                      // 当前页码
                    totalPages                       // 总页数
            );

            // 发送分页消息
            DocIeInferEvent<DocIeInferMessage> ocrEvent = new DocIeInferEvent<>(docIeInferMessage,
                    EventType.DOC_IE_INFER);
            ocrEvent.setDescription(String.format("文件 %s 第 %d 页实体识别知识抽取", fileId, pageNumber));
            applicationEventPublisher.publishEvent(ocrEvent);
        }

        return String.format("已发送 %d 个分页消息进行知识图谱提取，文件ID: %s", totalPages, fileId);
    }

}
