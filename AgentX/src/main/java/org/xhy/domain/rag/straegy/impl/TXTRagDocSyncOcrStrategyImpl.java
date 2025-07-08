package org.xhy.domain.rag.straegy.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dromara.streamquery.stream.core.bean.BeanHelper;
import org.dromara.streamquery.stream.core.stream.Steam;
import org.xhy.infrastructure.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.message.RagDocSyncOcrMessage;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.repository.FileDetailRepository;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import jakarta.annotation.Resource;

/**
 * @author shilong.zang
 * @date 19:07 <br/>
 */
@Service("ragDocSyncOcr-TXT")
public class TXTRagDocSyncOcrStrategyImpl extends RagDocSyncOcrStrategyImpl{


    private static final Logger log = LoggerFactory.getLogger(TXTRagDocSyncOcrStrategyImpl.class);

    private final DocumentUnitRepository documentUnitRepository;

    private final FileDetailRepository fileDetailRepository;

    private final StorageService storageService;

    public TXTRagDocSyncOcrStrategyImpl(DocumentUnitRepository documentUnitRepository, 
            FileDetailRepository fileDetailRepository, StorageService storageService) {
        this.documentUnitRepository = documentUnitRepository;
        this.fileDetailRepository = fileDetailRepository;
        this.storageService = storageService;
    }


    /**
     * 获取文件页数
     *
     * @param bytes
     * @param ragDocSyncOcrMessage
     */
    @Override
    public void pushPageSize(byte[] bytes, RagDocSyncOcrMessage ragDocSyncOcrMessage) {

    }

    /**
     * 获取文件
     *
     * @param ragDocSyncOcrMessage 消息数据
     * @param strategy             当前策略
     */
    @Override
    public byte[] getFileData(RagDocSyncOcrMessage ragDocSyncOcrMessage, String strategy) {
        // 从数据库中获取文件详情
        FileDetailEntity fileDetailEntity = fileDetailRepository.selectById(ragDocSyncOcrMessage.getFileId());
        if (fileDetailEntity == null) {
            log.error("File does not exist: {}", ragDocSyncOcrMessage.getFileId());
            return new byte[0];
        }

        // 使用文件路径下载文件
        log.info("Preparing to download TXT document: {}", fileDetailEntity.getFilename());
        return storageService.downloadFile(fileDetailEntity.getPath());
    }

    /**
     * ocr数据
     *
     * @param fileBytes
     * @param totalPages
     */
    @Override
    public Map<Integer, String> processFile(byte[] fileBytes, int totalPages) {
        log.info("Current type is non-PDF file, directly extract text ——————> Does not include page numbers, page number concept is index");

        DocumentParser parser = new TextDocumentParser();
        // 使用ByteArrayInputStream将字节数组转换为输入流
        InputStream inputStream = new ByteArrayInputStream(fileBytes);

        Document document;

        final HashMap<Integer, String> ocrData = new HashMap<>();

        try {
            document = parser.parse(inputStream);

            final DocumentBySentenceSplitter documentByCharacterSplitter = new DocumentBySentenceSplitter(500, 0);
            final List<TextSegment> split = documentByCharacterSplitter.split(document);

            Steam.of(split).forEachIdx((textSegment, index) -> {
                final String text = textSegment.text();

                ocrData.put(index, text);

            });

            return ocrData;

        } catch (Exception e) {
            log.error("Failed to process document", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.error("Failed to close the input stream", e);
            }
        }

        return null;
    }

    /**
     * 保存数据
     *
     * @param ragDocSyncOcrMessage
     * @param ocrData
     */
    @Override
    public void insertData(RagDocSyncOcrMessage ragDocSyncOcrMessage, Map<Integer, String> ocrData) throws Exception {

        log.info("Start saving document content, split into {} segments in total.", ragDocSyncOcrMessage.getPageSize());

        // 遍历每一页，将内容保存到数据库
        for (int pageIndex = 0; pageIndex < ragDocSyncOcrMessage.getPageSize(); pageIndex++) {
            String content = ocrData.getOrDefault(pageIndex, null);

            DocumentUnitEntity documentUnitEntity = new DocumentUnitEntity();
            documentUnitEntity.setContent(content);
            documentUnitEntity.setPage(pageIndex);
            documentUnitEntity.setFileId(ragDocSyncOcrMessage.getFileId());
            documentUnitEntity.setVector(false);
            documentUnitEntity.setOcr(true);

            if (content == null) {
                documentUnitEntity.setOcr(false);
                log.warn("Page {} is empty", pageIndex + 1);
            }

            // 保存或更新数据
            documentUnitRepository.checkInsert(documentUnitEntity);
            log.debug("Saving page {} content completed.", pageIndex + 1);
        }

        log.info("Word document content saved successfully");

    }
}
