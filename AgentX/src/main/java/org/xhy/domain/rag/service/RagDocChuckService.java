package org.xhy.domain.rag.service;

import org.dromara.streamquery.stream.core.bean.BeanHelper;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.message.RagDocSyncOcrMessage;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.repository.FileDetailRepository;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.config.ProviderConfig;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;
import org.xhy.infrastructure.mq.enums.EventType;
import org.xhy.infrastructure.mq.events.RagDocSyncStorageEvent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * @author shilong.zang
 * @date 23:38 <br/>
 */
@Service
public class RagDocChuckService {



    private final ApplicationContext applicationContext;


    public RagDocChuckService(ApplicationContext applicationContext) {
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

        final ChatLanguageModel ocrModel = LLMProviderService.getNormal(ProviderProtocol.OpenAI, null);

        final ChatResponse chat = ocrModel.chat();


        // 1. 获取文件id
        // 2. 获取文件
        // 3. 异步入库
    }

    private getPage
}
