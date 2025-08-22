package org.xhy.application.file.service;

import java.util.Arrays;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.application.file.factory.FileStorageStrategyFactory;
import org.xhy.application.file.strategy.FileStorageStrategy;
import org.xhy.domain.file.constant.FileTypeEnum;

import cn.hutool.core.util.StrUtil;

/** 文件存储应用服务
 * 
 * 实现 X-File-Storage 的 FileRecorder 接口，作为文件存储的统一入口， 根据文件类型选择合适的处理策略
 * 
 * @author shilong.zang
 * @date 2024-12-09 */
@Service
public class FileStorageAppService implements FileRecorder {

    private static final Logger log = LoggerFactory.getLogger(FileStorageAppService.class);

    private final FileStorageStrategyFactory strategyFactory;

    public FileStorageAppService(FileStorageStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    @Override
    public boolean save(FileInfo fileInfo) {
        try {
            log.info("开始保存文件: {}, 路径: {}", fileInfo.getOriginalFilename(), fileInfo.getPath());

            // 判断文件类型
            FileTypeEnum fileType = determineFileType(fileInfo);
            log.debug("文件类型判断结果: {}", fileType);

            // 获取对应的处理策略
            FileStorageStrategy strategy = strategyFactory.getStrategy(fileType);

            // 委托策略处理
            boolean result = strategy.save(fileInfo);

            log.info("文件保存{}：{}", result ? "成功" : "失败", fileInfo.getOriginalFilename());
            return result;

        } catch (Exception e) {
            log.error("保存文件失败：{}", fileInfo.getOriginalFilename(), e);
            throw new RuntimeException("保存文件失败", e);
        }
    }

    @Override
    public void update(FileInfo fileInfo) {
        try {
            log.info("开始更新文件: {}", fileInfo.getOriginalFilename());

            // 判断文件类型
            FileTypeEnum fileType = determineFileType(fileInfo);

            // 获取对应的处理策略
            FileStorageStrategy strategy = strategyFactory.getStrategy(fileType);

            // 委托策略处理
            strategy.update(fileInfo);

            log.info("文件更新成功：{}", fileInfo.getOriginalFilename());

        } catch (Exception e) {
            log.error("更新文件失败：{}", fileInfo.getOriginalFilename(), e);
            throw new RuntimeException("更新文件失败", e);
        }
    }

    @Override
    public FileInfo getByUrl(String url) {
        try {
            log.debug("根据URL查询文件: {}", url);

            // 判断文件类型（通过URL路径）
            FileTypeEnum fileType = determineFileTypeByUrl(url);

            // 获取对应的处理策略
            FileStorageStrategy strategy = strategyFactory.getStrategy(fileType);

            // 委托策略处理
            FileInfo result = strategy.getByUrl(url);

            log.debug("查询文件结果：{}", result != null ? "找到" : "未找到");
            return result;

        } catch (Exception e) {
            log.error("根据URL查询文件失败：{}", url, e);
            throw new RuntimeException("查询文件失败", e);
        }
    }

    @Override
    public boolean delete(String url) {
        try {
            log.info("开始删除文件: {}", url);

            // 判断文件类型（通过URL路径）
            FileTypeEnum fileType = determineFileTypeByUrl(url);

            // 获取对应的处理策略
            FileStorageStrategy strategy = strategyFactory.getStrategy(fileType);

            // 委托策略处理
            boolean result = strategy.delete(url);

            log.info("文件删除{}：{}", result ? "成功" : "失败", url);
            return result;

        } catch (Exception e) {
            log.error("删除文件失败：{}", url, e);
            throw new RuntimeException("删除文件失败", e);
        }
    }

    @Override
    public void saveFilePart(FilePartInfo filePartInfo) {
        // 文件分片功能暂不实现
        log.debug("文件分片保存请求（暂未实现）：{}", filePartInfo.getUploadId());
    }

    @Override
    public void deleteFilePartByUploadId(String uploadId) {
        // 文件分片功能暂不实现
        log.debug("文件分片删除请求（暂未实现）：{}", uploadId);
    }

    /** 根据文件信息判断文件类型
     * 
     * @param fileInfo 文件信息
     * @return 文件类型 */
    private FileTypeEnum determineFileType(FileInfo fileInfo) {

        // 通过文件扩展名判断
        String originalFilename = fileInfo.getOriginalFilename();
        if (StrUtil.isNotBlank(originalFilename)) {
            String suffix = extractFileExtension(originalFilename);
            if (StrUtil.isNotBlank(suffix)) {
                // RAG 文档类型
                if (Arrays.asList("pdf", "doc", "docx", "md", "txt", "html", "xml").contains(suffix.toLowerCase())) {
                    return FileTypeEnum.RAG;
                }
                // 头像图片类型
                if (Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp").contains(suffix.toLowerCase())) {
                    return FileTypeEnum.AVATAR;
                }
            }
        }

        // 4. 默认为通用文件
        return FileTypeEnum.GENERAL;
    }

    /** 提取文件扩展名
     * 
     * @param filename 文件名
     * @return 文件扩展名 */
    private String extractFileExtension(String filename) {
        if (StrUtil.isBlank(filename)) {
            return null;
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }

        return null;
    }

    /** 根据文件URL判断文件类型
     * 
     * @param url 文件URL
     * @return 文件类型 */
    private FileTypeEnum determineFileTypeByUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return FileTypeEnum.GENERAL;
        }

        // 通过URL路径模式判断
        if (url.contains("/rag/") || url.contains("/documents/")) {
            return FileTypeEnum.RAG;
        }
        if (url.contains("/avatar/") || url.contains("/user/")) {
            return FileTypeEnum.AVATAR;
        }

        // 通过URL中的文件扩展名判断
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.matches(".*\\.(pdf|doc|docx|md|txt|html|xml).*")) {
            return FileTypeEnum.RAG;
        }
        if (lowerUrl.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp).*")) {
            return FileTypeEnum.AVATAR;
        }

        return FileTypeEnum.GENERAL;
    }
}