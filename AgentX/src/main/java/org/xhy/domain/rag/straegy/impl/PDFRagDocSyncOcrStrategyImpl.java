package org.xhy.domain.rag.straegy.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.dromara.streamquery.stream.core.bean.BeanHelper;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.constant.PromptConstant;
import org.xhy.domain.rag.message.RagDocSyncOcrMessage;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.repository.FileDetailRepository;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;
import org.xhy.infrastructure.rag.detector.TikaFileTypeDetector;
import org.xhy.infrastructure.rag.utils.PdfToBase64Converter;

import cn.hutool.core.codec.Base64;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;

/**
 * @author shilong.zang
 * @date 10:20 <br/>
 */
@Service(value = "ragDocSyncOcr-PDF")
public class PDFRagDocSyncOcrStrategyImpl extends RagDocSyncOcrStrategyImpl implements PromptConstant {

    private static final Logger log = LoggerFactory.getLogger(PDFRagDocSyncOcrStrategyImpl.class);


    private final DocumentUnitRepository documentUnitRepository;

    private final FileDetailRepository fileDetailRepository;

    @Resource
    private FileStorageService fileStorageService;

    public PDFRagDocSyncOcrStrategyImpl(DocumentUnitRepository documentUnitRepository, FileDetailRepository fileDetailRepository) {
        this.documentUnitRepository = documentUnitRepository;
        this.fileDetailRepository = fileDetailRepository;
    }

    /**
     * 获取文件页数
     *
     */
    @Override
    public void pushPageSize(byte[] bytes,RagDocSyncOcrMessage ragDocSyncOcrMessage) {

        try {
            final int pdfPageCount = PdfToBase64Converter.getPdfPageCount(bytes);
            ragDocSyncOcrMessage.setPageSize(pdfPageCount);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 获取文件
     *
     * @param ragDocSyncOcrMessage 消息数据
     * @param strategy             当前策略
     */
    @Override
    public byte[] getFileData(RagDocSyncOcrMessage ragDocSyncOcrMessage, String strategy) {

        final FileDetailEntity fileDetailEntity = fileDetailRepository.selectById(ragDocSyncOcrMessage.getFileId());

        final FileInfo fileInfo = BeanHelper.copyProperties(fileDetailEntity, FileInfo.class);

        return fileStorageService.download(fileInfo).bytes();
    }

    /**
     * 处理PDF文件 - 按页处理逻辑
     */
    @Override
    public Map<Integer,String> processFile(byte[] fileBytes, int totalPages) {

        final HashMap<Integer, String> ocrData = new HashMap<>();
        for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
            try {
                // 单独处理每一页以减少内存使用
                String base64 = PdfToBase64Converter.processPdfPageToBase64(fileBytes, pageIndex, "jpg");

                final UserMessage userMessage = UserMessage.userMessage(
                        ImageContent.from(base64, TikaFileTypeDetector.detectFileType(Base64.decode(base64))),
                        TextContent.from(OCR_PROMPT)
                );


                final ChatLanguageModel ocrModel = LLMProviderService.getNormal(ProviderProtocol.OpenAI, null);

                final ChatResponse chat = ocrModel.chat(userMessage);

                ocrData.put(pageIndex, chat.aiMessage().text());

                log.info("第{}页处理请求，共{}页，当前内存使用: {}MB",
                        (pageIndex + 1),
                        totalPages,
                        (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024));

                if ((pageIndex + 1) % 10 == 0) {
                    System.gc();
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                log.info("第{}页处理完成", (pageIndex + 1));
            } catch (Exception e) {
                log.error("处理PDF第{}页时发生错误: {}", (pageIndex + 1), e.getMessage());
                // 继续处理下一页，不中断整个流程
            }
        }

        return ocrData;

    }

    /**
     * 保存数据
     *
     * @param ragDocSyncOcrMessage 消息数据
     * @param ocrData ocr数据
     */
    @Override
    public void insertData(RagDocSyncOcrMessage ragDocSyncOcrMessage, Map<Integer, String> ocrData) {

        for (int pageIndex = 0; pageIndex < ragDocSyncOcrMessage.getPageSize(); pageIndex++) {

            String content = ocrData.getOrDefault(pageIndex,null);

            final DocumentUnitEntity documentUnitDO = new DocumentUnitEntity();

            documentUnitDO.setContent(content);
            documentUnitDO.setPage(pageIndex);
            documentUnitDO.setFileId(ragDocSyncOcrMessage.getFileId());
            documentUnitDO.setVector(false);
            documentUnitDO.setOcr(true);

            if (content == null) {
                documentUnitDO.setOcr(false);
            }

            documentUnitRepository.checkInsert(documentUnitDO);

        }
    }
}
