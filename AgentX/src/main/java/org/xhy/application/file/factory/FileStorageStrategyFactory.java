package org.xhy.application.file.factory;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.xhy.application.file.strategy.FileStorageStrategy;
import org.xhy.domain.file.constant.FileTypeEnum;

/** 文件存储策略工厂
 * 
 * 根据文件类型返回对应的处理策略
 * 
 * @author shilong.zang
 * @date 2024-12-09 */
@Component
public class FileStorageStrategyFactory {

    private final Map<FileTypeEnum, FileStorageStrategy> strategyMap;

    public FileStorageStrategyFactory(Map<FileTypeEnum, FileStorageStrategy> strategyMap) {
        this.strategyMap = strategyMap;
    }

    /** 根据文件类型获取对应的处理策略
     * 
     * @param fileType 文件类型
     * @return 文件存储策略 */
    public FileStorageStrategy getStrategy(FileTypeEnum fileType) {
        FileStorageStrategy strategy = strategyMap.get(fileType);

        if (strategy == null) {
            // 如果没有找到对应策略，返回通用策略
            strategy = strategyMap.get(FileTypeEnum.GENERAL);
        }

        if (strategy == null) {
            throw new IllegalStateException("未找到文件类型 " + fileType + " 对应的处理策略");
        }

        return strategy;
    }

    /** 获取默认策略（通用文件策略）
     * 
     * @return 默认文件存储策略 */
    public FileStorageStrategy getDefaultStrategy() {
        return getStrategy(FileTypeEnum.GENERAL);
    }
}