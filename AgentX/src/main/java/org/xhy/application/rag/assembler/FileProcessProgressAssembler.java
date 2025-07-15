package org.xhy.application.rag.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.rag.dto.FileProcessProgressDTO;
import org.xhy.domain.rag.constant.EmbeddingStatusEnum;
import org.xhy.domain.rag.constant.FileInitializeStatusEnum;
import org.xhy.domain.rag.constant.FileProcessStatusEnum;
import org.xhy.domain.rag.model.FileDetailEntity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.xhy.domain.rag.constant.FileProcessStatusEnum.getEmbeddingStatusDescription;

/** 文件处理进度转换器
 * @author shilong.zang
 * @date 2025-01-15 */
public class FileProcessProgressAssembler {

    /** 实体转换为DTO
     * @param entity 文件实体
     * @return 处理进度DTO */
    public static FileProcessProgressDTO toDTO(FileDetailEntity entity) {
        if (entity == null) {
            return null;
        }

        FileProcessProgressDTO dto = new FileProcessProgressDTO();
        dto.setFileId(entity.getId());
        dto.setFilename(entity.getOriginalFilename());

        // 设置新的枚举状态字段
        dto.setInitializeStatusEnum(FileInitializeStatusEnum.fromCode(entity.getIsInitialize()));
        dto.setEmbeddingStatusEnum(EmbeddingStatusEnum.fromCode(entity.getIsEmbedding()));

        // 设置中文状态字段（保持兼容性）
        dto.setInitializeStatus(FileProcessStatusEnum.getInitStatusDescription(entity.getIsInitialize()));
        dto.setEmbeddingStatus(getEmbeddingStatusDescription(entity.getIsEmbedding()));

        // 设置分离的页数和进度
        dto.setCurrentOcrPageNumber(entity.getCurrentOcrPageNumber() != null ? entity.getCurrentOcrPageNumber() : 0);
        dto.setCurrentEmbeddingPageNumber(
                entity.getCurrentEmbeddingPageNumber() != null ? entity.getCurrentEmbeddingPageNumber() : 0);
        dto.setFilePageSize(entity.getFilePageSize() != null ? entity.getFilePageSize() : 0);
        dto.setOcrProcessProgress(entity.getOcrProcessProgress() != null ? entity.getOcrProcessProgress() : 0.0);
        dto.setEmbeddingProcessProgress(
                entity.getEmbeddingProcessProgress() != null ? entity.getEmbeddingProcessProgress() : 0.0);

        // 设置兼容性字段
        dto.setIsInitialize(entity.getIsInitialize());
        dto.setIsEmbedding(entity.getIsEmbedding());
        dto.setCurrentPageNumber(entity.getCurrentOcrPageNumber() != null ? entity.getCurrentOcrPageNumber() : 0);
        dto.setProcessProgress(entity.getOcrProcessProgress() != null ? entity.getOcrProcessProgress() : 0.0);

        dto.setStatusDescription(getStatusDescription(entity));
        return dto;
    }

    /** 实体列表转换为DTO列表
     * @param entities 文件实体列表
     * @return 处理进度DTO列表 */
    public static List<FileProcessProgressDTO> toDTOs(List<FileDetailEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(FileProcessProgressAssembler::toDTO).collect(Collectors.toList());
    }

    /** 获取状态描述
     * @param entity 文件实体
     * @return 状态描述 */
    private static String getStatusDescription(FileDetailEntity entity) {
        Integer initStatus = entity.getIsInitialize();
        Integer embeddingStatus = entity.getIsEmbedding();

        if (initStatus == null || initStatus == 0) {
            return "待初始化";
        } else if (initStatus == 1) {
            return "初始化中";
        } else if (initStatus == 3) {
            return "初始化失败";
        } else if (initStatus == 2) {
            if (embeddingStatus == null || embeddingStatus == 0) {
                return "初始化完成，待向量化";
            } else if (embeddingStatus == 1) {
                return "向量化中";
            } else if (embeddingStatus == 3) {
                return "向量化失败";
            } else if (embeddingStatus == 2) {
                return "处理完成";
            }
        }
        return "未知状态";
    }
}