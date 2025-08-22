package org.xhy.application.file.strategy;

import org.dromara.x.file.storage.core.FileInfo;

/** 文件存储策略接口
 * 
 * 定义不同类型文件的处理策略，每种文件类型可以有不同的处理逻辑
 * 
 * @author shilong.zang
 * @date 2024-12-09 */
public interface FileStorageStrategy {

    /** 保存文件信息
     * 
     * @param fileInfo 文件信息
     * @return 是否保存成功 */
    boolean save(FileInfo fileInfo);

    /** 更新文件信息
     * 
     * @param fileInfo 文件信息 */
    void update(FileInfo fileInfo);

    /** 根据URL查询文件信息
     * 
     * @param url 文件URL
     * @return 文件信息 */
    FileInfo getByUrl(String url);

    /** 根据URL删除文件
     * 
     * @param url 文件URL
     * @return 是否删除成功 */
    boolean delete(String url);
}