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
     * 生成图谱
     * @param fileId 文件ID
     * @return 图谱生成结果
     */
    public String generateGraph(String fileId) {

        final List<DocumentUnitEntity> documentUnitEntities = documentUnitDomainService.listDocumentUnitsByFileId(
                fileId);

        if (documentUnitEntities == null || documentUnitEntities.isEmpty()) {
            throw new IllegalArgumentException("No document units found for fileId: " + fileId);
        }

        final DocIeInferMessage docIeInferMessage = new DocIeInferMessage();

        docIeInferMessage.setFileId(fileId);
        docIeInferMessage.setDocumentText(Steam.of(documentUnitEntities).map(DocumentUnitEntity::getContent).join());

        DocIeInferEvent<DocIeInferMessage> ocrEvent = new DocIeInferEvent<>(docIeInferMessage,
                EventType.DOC_IE_INFER);
        ocrEvent.setDescription("文件实体识别知识抽取");
        applicationEventPublisher.publishEvent(ocrEvent);

        return docIeInferMessage.getDocumentText();
    }

}
