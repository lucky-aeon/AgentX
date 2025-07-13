package org.xhy.domain.rag.constant;

/** @author zang
 * @date 10:19 <br/>
 */
public interface EmbeddingStatus {

    /** 未初始化 */
    Integer UNINITIALIZED = 0;
    /** 初始化中 */
    Integer INITIALIZING = 1;
    /** 已初始化 */
    Integer INITIALIZED = 2;

    /** 初始化失败 */
    Integer INITIALIZATION_FAILED = 3;

}
