package org.xhy.domain.rag.strategy.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.dromara.streamquery.stream.core.stream.Steam;
import org.dromara.x.file.storage.core.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.message.RagDocMessage;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.repository.FileDetailRepository;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import jakarta.annotation.Resource;

@Service("txt")
public class TXTRagDocSyncOcrStrategyImpl extends AbstractDocumentProcessingStrategy {

    private static final Logger log = LoggerFactory.getLogger(TXTRagDocSyncOcrStrategyImpl.class);

    private final DocumentUnitRepository documentUnitRepository;

    private final FileDetailRepository fileDetailRepository;

    @Resource
    private FileStorageService fileStorageService;

    // 用于存储当前处理的文件ID，以便更新页数
    private String currentProcessingFileId;

    public TXTRagDocSyncOcrStrategyImpl(DocumentUnitRepository documentUnitRepository,
            FileDetailRepository fileDetailRepository) {
        this.documentUnitRepository = documentUnitRepository;
        this.fileDetailRepository = fileDetailRepository;
    }

    /** 处理消息，设置当前处理的文件ID
     * @param ragDocSyncOcrMessage 消息数据
     * @param strategy 当前策略 */
    @Override
    public void handle(RagDocMessage ragDocSyncOcrMessage, String strategy) throws Exception {
        // 设置当前处理的文件ID，用于更新页数
        this.currentProcessingFileId = ragDocSyncOcrMessage.getFileId();

        // 调用父类处理逻辑
        super.handle(ragDocSyncOcrMessage, strategy);
    }

    /** 获取文件页数
     *
     * @param bytes
     * @param ragDocSyncOcrMessage */
    @Override
    public void pushPageSize(byte[] bytes, RagDocMessage ragDocSyncOcrMessage) {
        try {
            DocumentParser parser = new TextDocumentParser();
            InputStream inputStream = new ByteArrayInputStream(bytes);
            Document document = parser.parse(inputStream);

            final DocumentBySentenceSplitter documentByCharacterSplitter = new DocumentBySentenceSplitter(500, 0);
            final List<TextSegment> split = documentByCharacterSplitter.split(document);

            int segmentCount = split.size();
            ragDocSyncOcrMessage.setPageSize(segmentCount);
            log.info("TXT document split into {} segments", segmentCount);

            // 更新数据库中的总页数
            if (currentProcessingFileId != null) {
                LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                        .eq(FileDetailEntity::getId, currentProcessingFileId)
                        .set(FileDetailEntity::getFilePageSize, segmentCount);
                fileDetailRepository.update(wrapper);

                log.info("Updated total pages for TXT file {}: {} segments", currentProcessingFileId, segmentCount);
            }

            inputStream.close();
        } catch (Exception e) {
            log.error("Failed to calculate page size for TXT document", e);
            ragDocSyncOcrMessage.setPageSize(0);
        }
    }

    /** 获取文件
     *
     * @param ragDocSyncOcrMessage 消息数据
     * @param strategy 当前策略 */
    @Override
    public byte[] getFileData(RagDocMessage ragDocSyncOcrMessage, String strategy) {
        // 从数据库中获取文件详情
        FileDetailEntity fileDetailEntity = fileDetailRepository.selectById(ragDocSyncOcrMessage.getFileId());
        if (fileDetailEntity == null) {
            log.error("File does not exist: {}", ragDocSyncOcrMessage.getFileId());
            return new byte[0];
        }

        // 转换为FileInfo并下载文件
        log.info("Preparing to download TXT document: {}", fileDetailEntity.getFilename());
        return fileStorageService.download(fileDetailEntity.getUrl()).bytes();
    }

    /** ocr数据
     *
     * @param fileBytes
     * @param totalPages */
    @Override
    public Map<Integer, String> processFile(byte[] fileBytes, int totalPages) {
        log.info(
                "Current type is non-PDF file, directly extract text ——————> Does not include page numbers, page number concept is index");

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

    /** 保存数据
     *
     * @param ragDocSyncOcrMessage
     * @param ocrData */
    @Override
    public void insertData(RagDocMessage ragDocSyncOcrMessage, Map<Integer, String> ocrData) throws Exception {

        log.info("Start saving document content, split into {} segments in total.", ocrData.size());

        // 遍历每一页，将内容保存到数据库
        for (int pageIndex = 0; pageIndex < ocrData.size(); pageIndex++) {
            String content = ocrData.getOrDefault(pageIndex, null);

            DocumentUnitEntity documentUnitEntity = new DocumentUnitEntity();
            documentUnitEntity.setContent(content);
            documentUnitEntity.setPage(pageIndex);
            documentUnitEntity.setFileId(ragDocSyncOcrMessage.getFileId());
            documentUnitEntity.setIsVector(false);
            documentUnitEntity.setIsOcr(true);

            if (content == null) {
                documentUnitEntity.setIsOcr(false);
                log.warn("Page {} is empty", pageIndex + 1);
            }

            // 保存或更新数据
            documentUnitRepository.checkInsert(documentUnitEntity);
            log.debug("Saving page {} content completed.", pageIndex + 1);
        }

        log.info("TXT document content saved successfully");

    }
}
