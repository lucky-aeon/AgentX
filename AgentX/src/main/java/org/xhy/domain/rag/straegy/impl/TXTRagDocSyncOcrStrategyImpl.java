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

    @Resource
    private FileStorageService fileStorageService;

    public TXTRagDocSyncOcrStrategyImpl(DocumentUnitRepository documentUnitRepository, FileDetailRepository fileDetailRepository) {
        this.documentUnitRepository = documentUnitRepository;
        this.fileDetailRepository = fileDetailRepository;
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
            log.error("文件不存在: {}", ragDocSyncOcrMessage.getFileId());
            return new byte[0];
        }

        // 转换为FileInfo并下载文件
        FileInfo fileInfo = BeanHelper.copyProperties(fileDetailEntity, FileInfo.class);
        log.info("准备下载Word文档: {}", fileInfo.getFilename());
        return fileStorageService.download(fileInfo).bytes();
    }

    /**
     * ocr数据
     *
     * @param fileBytes
     * @param totalPages
     */
    @Override
    public Map<Integer, String> processFile(byte[] fileBytes, int totalPages) {
        log.info("当前类型为非pdf文件，直接提取文字 ——————> 不包含页数，页数概念为索引");

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
            log.error("处理文档失败", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.error("关闭输入流失败", e);
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
