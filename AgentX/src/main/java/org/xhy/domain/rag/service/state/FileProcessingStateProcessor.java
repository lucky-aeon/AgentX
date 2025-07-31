package org.xhy.domain.rag.service.state;

import org.xhy.domain.rag.model.FileDetailEntity;

/** 文件处理状态处理器接口
 * 
 * @author zang
 * @date 2025-07-17 */
public interface FileProcessingStateProcessor {

    /** 获取处理器对应的状态
     * 
     * @return 文件处理状态 */
    Integer getStatus();

    /** 处理文件状态
     * 
     * @param fileEntity 文件实体 */
    void process(FileDetailEntity fileEntity);

    /** 获取下一个可能的状态（可选实现）
     * 
     * @return 下一个状态数组 */
    default Integer[] getNextPossibleStatuses() {
        return new Integer[0];
    }

    /** 是否可以转换到目标状态
     * 
     * @param targetStatus 目标状态
     * @return 是否可以转换 */
    default boolean canTransitionTo(Integer targetStatus) {
        Integer[] nextStatuses = getNextPossibleStatuses();
        for (Integer status : nextStatuses) {
            if (status.equals(targetStatus)) {
                return true;
            }
        }
        return false;
    }
}