package org.xhy.domain.rag.straegy.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dromara.streamquery.stream.core.bean.BeanHelper;
import org.dromara.streamquery.stream.core.stream.Steam;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.constant.RAGSystemPrompt;
import org.xhy.domain.rag.message.RagDocSyncOcrMessage;
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

/**
 * Word文档处理策略实现
 * @author shilong.zang
 * @date 10:07 <br/>
 */
@Service(value = "ragDocSyncOcr-WORD")
public class WORDRagDocSyncOcrStrategyImpl extends RagDocSyncOcrStrategyImpl implements RAGSystemPrompt {

    private static final Logger log = LoggerFactory.getLogger(WORDRagDocSyncOcrStrategyImpl.class);

    private final DocumentUnitRepository documentUnitRepository;

    private final FileDetailRepository fileDetailRepository;

    @Resource
    private FileStorageService fileStorageService;

    public WORDRagDocSyncOcrStrategyImpl(DocumentUnitRepository documentUnitRepository, FileDetailRepository fileDetailRepository) {
        this.documentUnitRepository = documentUnitRepository;
        this.fileDetailRepository = fileDetailRepository;
    }

    /**
     * 获取文件页数
     *
     * @param bytes Word文档字节数组
     * @param ragDocSyncOcrMessage 消息数据
     */
    @Override
    public void pushPageSize(byte[] bytes, RagDocSyncOcrMessage ragDocSyncOcrMessage) {




    }

    /**
     * 获取文件数据
     *
     * @param ragDocSyncOcrMessage 消息数据
     * @param strategy             当前策略
     * @return Word文档字节数组
     */
    @Override
    public byte[] getFileData(RagDocSyncOcrMessage ragDocSyncOcrMessage, String strategy) {
        // 从数据库中获取文件详情
        FileDetailEntity fileDetailEntity = fileDetailRepository.selectById(ragDocSyncOcrMessage.getFileId());
        if (fileDetailEntity == null) {
            log.error("File not found: {}", ragDocSyncOcrMessage.getFileId());
            return new byte[0];
        }

        // 转换为FileInfo并下载文件
        FileInfo fileInfo = BeanHelper.copyProperties(fileDetailEntity, FileInfo.class);
        log.info("Preparing to download Word document: {}", fileInfo.getFilename());
        return fileStorageService.download(fileInfo).bytes();
    }

    /**
     * 处理Word文件 - 提取文本内容
     *
     * @param fileBytes  Word文档字节数组
     * @param totalPages 总页数
     * @return 按页索引分组的内容Map
     */
    @Override
    public Map<Integer, String> processFile(byte[] fileBytes, int totalPages) {
        log.info("Current file type is non-PDF, text is extracted directly ——————> Does not contain page numbers; the concept of page numbers serves as an index.");

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

    /**
     * 保存数据
     *
     * @param ragDocSyncOcrMessage 消息数据
     * @param ocrData              按页索引分组的内容Map
     */
    @Override
    public void insertData(RagDocSyncOcrMessage ragDocSyncOcrMessage, Map<Integer, String> ocrData) throws Exception {
        log.info("开始保存文档内容，共拆分{}段", ragDocSyncOcrMessage.getPageSize());

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
                log.warn("第{}页内容为空", pageIndex + 1);
            }
            
            // 保存或更新数据
            documentUnitRepository.checkInsert(documentUnitEntity);
            log.debug("保存第{}页内容完成", pageIndex + 1);
        }
        
        log.info("Word文档内容保存完成");
    }
}
