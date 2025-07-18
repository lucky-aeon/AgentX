package org.xhy.application.rag.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.rag.dto.FileProcessProgressDTO;
import org.xhy.domain.rag.constant.FileProcessingStatusEnum;
import org.xhy.domain.rag.model.FileDetailEntity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

        // 设置新的统一状态字段
        Integer processingStatus = entity.getProcessingStatus();
        if (processingStatus != null) {
            FileProcessingStatusEnum statusEnum = FileProcessingStatusEnum.fromCode(processingStatus);
            dto.setProcessingStatusEnum(statusEnum);
            dto.setProcessingStatus(processingStatus);
            dto.setProcessingStatusDescription(statusEnum.getDescription());
        }

        // 设置分离的页数和进度
        dto.setCurrentOcrPageNumber(entity.getCurrentOcrPageNumber() != null ? entity.getCurrentOcrPageNumber() : 0);
        dto.setCurrentEmbeddingPageNumber(
                entity.getCurrentEmbeddingPageNumber() != null ? entity.getCurrentEmbeddingPageNumber() : 0);
        dto.setFilePageSize(entity.getFilePageSize() != null ? entity.getFilePageSize() : 0);
        dto.setOcrProcessProgress(entity.getOcrProcessProgress() != null ? entity.getOcrProcessProgress() : 0.0);
        dto.setEmbeddingProcessProgress(
                entity.getEmbeddingProcessProgress() != null ? entity.getEmbeddingProcessProgress() : 0.0);

        // 设置兼容性字段 - 从统一状态映射到旧状态
        mapToLegacyStatus(dto, processingStatus);

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

    /** 映射到旧版本兼容性字段
     * @param dto 处理进度DTO
     * @param processingStatus 统一状态 */
    private static void mapToLegacyStatus(FileProcessProgressDTO dto, Integer processingStatus) {
        if (processingStatus == null) {
            dto.setIsInitialize(0);
            dto.setIsEmbedding(0);
            dto.setInitializeStatus("待初始化");
            dto.setEmbeddingStatus("待向量化");
            return;
        }

        FileProcessingStatusEnum statusEnum = FileProcessingStatusEnum.fromCode(processingStatus);
        
        switch (statusEnum) {
            case UPLOADED:
                dto.setIsInitialize(0);
                dto.setIsEmbedding(0);
                dto.setInitializeStatus("待初始化");
                dto.setEmbeddingStatus("待向量化");
                break;
            case OCR_PROCESSING:
                dto.setIsInitialize(1);
                dto.setIsEmbedding(0);
                dto.setInitializeStatus("初始化中");
                dto.setEmbeddingStatus("待向量化");
                break;
            case OCR_COMPLETED:
                dto.setIsInitialize(2);
                dto.setIsEmbedding(0);
                dto.setInitializeStatus("初始化完成");
                dto.setEmbeddingStatus("待向量化");
                break;
            case EMBEDDING_PROCESSING:
                dto.setIsInitialize(2);
                dto.setIsEmbedding(1);
                dto.setInitializeStatus("初始化完成");
                dto.setEmbeddingStatus("向量化中");
                break;
            case COMPLETED:
                dto.setIsInitialize(2);
                dto.setIsEmbedding(2);
                dto.setInitializeStatus("初始化完成");
                dto.setEmbeddingStatus("向量化完成");
                break;
            case OCR_FAILED:
                dto.setIsInitialize(3);
                dto.setIsEmbedding(0);
                dto.setInitializeStatus("初始化失败");
                dto.setEmbeddingStatus("待向量化");
                break;
            case EMBEDDING_FAILED:
                dto.setIsInitialize(2);
                dto.setIsEmbedding(3);
                dto.setInitializeStatus("初始化完成");
                dto.setEmbeddingStatus("向量化失败");
                break;
            default:
                dto.setIsInitialize(0);
                dto.setIsEmbedding(0);
                dto.setInitializeStatus("未知状态");
                dto.setEmbeddingStatus("未知状态");
                break;
        }
    }

    /** 获取状态描述
     * @param entity 文件实体
     * @return 状态描述 */
    private static String getStatusDescription(FileDetailEntity entity) {
        Integer processingStatus = entity.getProcessingStatus();
        if (processingStatus == null) {
            return "待初始化";
        }

        FileProcessingStatusEnum statusEnum = FileProcessingStatusEnum.fromCode(processingStatus);
        return statusEnum.getDescription();
    }
}