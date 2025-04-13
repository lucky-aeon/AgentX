package org.xhy.domain.rag.service;

import org.dromara.streamquery.stream.core.bean.BeanHelper;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.repository.FileDetailRepository;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.config.ProviderConfig;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * @author shilong.zang
 * @date 23:38 <br/>
 */
@Service
public class RagDocChuckService {

    private final DocumentUnitRepository documentUnitRepository;

    private final FileDetailRepository fileDetailRepository;

    private final FileStorageService fileStorageService;

    public RagDocChuckService(DocumentUnitRepository documentUnitRepository, FileDetailRepository fileDetailRepository, FileStorageService fileStorageService) {
        this.documentUnitRepository = documentUnitRepository;
        this.fileDetailRepository = fileDetailRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * 根据文件id开始入库数据
     * @param fileId 文件id
     */
    public void ragDocChuckInsert(String fileId) {

        final FileDetailEntity fileDetailEntity = fileDetailRepository.selectById(fileId);

        final FileInfo fileInfo = BeanHelper.copyProperties(fileDetailEntity, FileInfo.class);

        byte[] bytes = fileStorageService.download(fileInfo).bytes();



        final ChatLanguageModel ocrModel = LLMProviderService.getNormal(ProviderProtocol.OpenAI, null);

        final ChatResponse chat = ocrModel.chat();


        // 1. 获取文件id
        // 2. 获取文件
        // 3. 异步入库
    }
}
