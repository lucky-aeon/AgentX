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
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import jakarta.annotation.Resource;

@Service("word")
public class WORDDocumentProcessing extends AbstractDocumentProcessingStrategy {

    private static final Logger log = LoggerFactory.getLogger(WORDDocumentProcessing.class);

    private final DocumentUnitRepository documentUnitRepository;

    private final FileDetailRepository fileDetailRepository;

    @Resource
    private FileStorageService fileStorageService;

    // 用于存储当前处理的文件ID，以便更新页数
    private String currentProcessingFileId;

    public WORDDocumentProcessing(DocumentUnitRepository documentUnitRepository,
            FileDetailRepository fileDetailRepository) {
        this.documentUnitRepository = documentUnitRepository;
        this.fileDetailRepository = fileDetailRepository;
    }

    /** 处理消息，设置当前处理的文件ID
     * @param ragDocMessage 消息数据
     * @param strategy 当前策略 */
    @Override
    public void handle(RagDocMessage ragDocMessage, String strategy) throws Exception {
        // 设置当前处理的文件ID，用于更新页数
        this.currentProcessingFileId = ragDocMessage.getFileId();

        // 调用父类处理逻辑
        super.handle(ragDocMessage, strategy);
    }

    /** 获取文件页数
     *
     * @param bytes Word文档字节数组
     * @param ragDocMessage 消息数据 */
    @Override
    public void pushPageSize(byte[] bytes, RagDocMessage ragDocMessage) {
        try {
            DocumentParser parser = new ApachePoiDocumentParser();
            InputStream inputStream = new ByteArrayInputStream(bytes);
            Document document = parser.parse(inputStream);

            final DocumentBySentenceSplitter documentByCharacterSplitter = new DocumentBySentenceSplitter(500, 0);
            final List<TextSegment> split = documentByCharacterSplitter.split(document);

            int segmentCount = split.size();
            ragDocMessage.setPageSize(segmentCount);
            log.info("Word document split into {} segments", segmentCount);

            // 更新数据库中的总页数
            if (currentProcessingFileId != null) {
                LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                        .eq(FileDetailEntity::getId, currentProcessingFileId)
                        .set(FileDetailEntity::getFilePageSize, segmentCount);
                fileDetailRepository.update(wrapper);

                log.info("Updated total pages for Word file {}: {} segments", currentProcessingFileId, segmentCount);
            }

            inputStream.close();
        } catch (Exception e) {
            log.error("Failed to calculate page size for Word document", e);
            ragDocMessage.setPageSize(0);
        }
    }

    /** 获取文件数据
     *
     * @param ragDocSyncOcrMessage 消息数据
     * @param strategy 当前策略
     * @return Word文档字节数组 */
    @Override
    public byte[] getFileData(RagDocMessage ragDocSyncOcrMessage, String strategy) {
        // 从数据库中获取文件详情
        FileDetailEntity fileDetailEntity = fileDetailRepository.selectById(ragDocSyncOcrMessage.getFileId());
        if (fileDetailEntity == null) {
            log.error("File not found: {}", ragDocSyncOcrMessage.getFileId());
            return new byte[0];
        }

        log.info("Preparing to download Word document: {}", fileDetailEntity.getFilename());
        return fileStorageService.download(fileDetailEntity.getUrl()).bytes();
    }

    /** 处理Word文件 - 提取文本内容
     *
     * @param fileBytes Word文档字节数组
     * @param totalPages 总页数
     * @return 按页索引分组的内容Map */
    @Override
    public Map<Integer, String> processFile(byte[] fileBytes, int totalPages) {
        log.info(
                "Current file type is non-PDF, text is extracted directly ——————> Does not contain page numbers; the concept of page numbers serves as an index.");

        DocumentParser parser = new ApachePoiDocumentParser();
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

        return ocrData;
    }

    /** 保存数据
     *
     * @param ragDocSyncOcrMessage 消息数据
     * @param ocrData 按页索引分组的内容Map */
    @Override
    public void insertData(RagDocMessage ragDocSyncOcrMessage, Map<Integer, String> ocrData) throws Exception {
        log.info("开始保存文档内容，共拆分{}段", ocrData.size());

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
                log.warn("第{}页内容为空", pageIndex + 1);
            }

            // 保存或更新数据
            documentUnitRepository.checkInsert(documentUnitEntity);
            log.debug("保存第{}页内容完成", pageIndex + 1);
        }

        log.info("Word文档内容保存完成");
    }
}
