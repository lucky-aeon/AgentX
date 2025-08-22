package org.xhy.domain.rag.strategy.impl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.domain.rag.message.RagDocMessage;
import org.xhy.domain.rag.strategy.DocumentProcessingStrategy;

public abstract class AbstractDocumentProcessingStrategy implements DocumentProcessingStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDocumentProcessingStrategy.class);

    /** 处理消息
     * @param ragDocSyncOcrMessage 消息数据
     * @param strategy 当前策略 */
    @Override
    public void handle(RagDocMessage ragDocSyncOcrMessage, String strategy) throws Exception {

        final byte[] fileData = getFileData(ragDocSyncOcrMessage, strategy);
        pushPageSize(fileData, ragDocSyncOcrMessage);
        if (fileData == null) {
            LOG.error("File data is empty");
            return;
        }

        Integer pageSize = ragDocSyncOcrMessage.getPageSize();
        if (pageSize == null) {
            LOG.warn("Page size is null, using default value 1 for txt/word files");
            pageSize = 1;
        }
        final Map<Integer, String> ocrData = processFile(fileData, pageSize, ragDocSyncOcrMessage);

        LOG.info("Successfully retrieved {} pages of data from the current file", ocrData.size());

        insertData(ragDocSyncOcrMessage, ocrData);

    };

    /** 获取文件页数 */
    abstract public void pushPageSize(byte[] bytes, RagDocMessage ragDocSyncOcrMessage);

    /** 获取文件
     * @param ragDocSyncOcrMessage 消息数据
     * @param strategy 当前策略 */
    abstract public byte[] getFileData(RagDocMessage ragDocSyncOcrMessage, String strategy);

    /** ocr数据 */
    abstract public Map<Integer, String> processFile(byte[] fileBytes, int totalPages);

    /** ocr数据 (带消息参数，子类可选择性重写此方法) */
    public Map<Integer, String> processFile(byte[] fileBytes, int totalPages, RagDocMessage ragDocSyncOcrMessage) {
        return processFile(fileBytes, totalPages);
    }

    /** 保存数据 */
    abstract public void insertData(RagDocMessage ragDocSyncOcrMessage, Map<Integer, String> ocrData) throws Exception;
}
