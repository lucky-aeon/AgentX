package org.xhy.domain.rag.service;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.message.RagDocSyncOcrMessage;
import org.xhy.infrastructure.mq.enums.EventType;
import org.xhy.infrastructure.mq.events.RagDocSyncStorageEvent;

/**
 * @author shilong.zang
 * @date 23:38 <br/>
 */
@Service
public class RagDocDomainChuckService {



    private final ApplicationContext applicationContext;


    public RagDocDomainChuckService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 根据文件id开始入库数据
     * @param fileId 文件id
     */
    public void ragDocChuckInsert(String fileId) {

        final RagDocSyncOcrMessage ragDocSyncOcrMessage = new RagDocSyncOcrMessage();

        ragDocSyncOcrMessage.setFileId(fileId);

        applicationContext.publishEvent(new RagDocSyncStorageEvent<>(ragDocSyncOcrMessage, EventType.DOC_REFRESH_ORG));

    }

}
