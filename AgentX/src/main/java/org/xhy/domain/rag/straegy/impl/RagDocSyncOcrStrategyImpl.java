package org.xhy.domain.rag.straegy.impl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.domain.rag.message.RagDocSyncOcrMessage;
import org.xhy.domain.rag.straegy.RagDocSyncOcrStrategy;

/** @author shilong.zang
 * @date 17:32 <br/>
 */
public abstract class RagDocSyncOcrStrategyImpl implements RagDocSyncOcrStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(RagDocSyncOcrStrategyImpl.class);

    /** 处理消息
     * @param ragDocSyncOcrMessage 消息数据
     * @param strategy 当前策略 */
    @Override
    public void handle(RagDocSyncOcrMessage ragDocSyncOcrMessage, String strategy) throws Exception {

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
        final Map<Integer, String> ocrData = processFile(fileData, pageSize);

        LOG.info("Successfully retrieved {} pages of data from the current file", ocrData.size());

        insertData(ragDocSyncOcrMessage, ocrData);

    };

    /** 获取文件页数 */
    abstract public void pushPageSize(byte[] bytes, RagDocSyncOcrMessage ragDocSyncOcrMessage);

    /** 获取文件
     * @param ragDocSyncOcrMessage 消息数据
     * @param strategy 当前策略 */
    abstract public byte[] getFileData(RagDocSyncOcrMessage ragDocSyncOcrMessage, String strategy);

    /** ocr数据 */
    abstract public Map<Integer, String> processFile(byte[] fileBytes, int totalPages);

    /** 保存数据 */
    abstract public void insertData(RagDocSyncOcrMessage ragDocSyncOcrMessage, Map<Integer, String> ocrData)
            throws Exception;
}
