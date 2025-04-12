package org.xhy.domain.rag.constant;

/**
 * @author zang
 * @date 10:19 <br/>
 */
public interface EmbeddingStatus {

    /**
     * 未初始化
     */
    String UNINITIALIZED = "未入库";
    /**
     * 初始化中
     */
    String INITIALIZING = "入库中";
    /**
     * 已初始化
     */
    String INITIALIZED = "已入库";

    /**
     * 初始化失败
     */
    String INITIALIZATION_FAILED = "入库失败";

}
