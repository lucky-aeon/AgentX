package org.xhy.application.rag.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.rag.dto.FileDetailDTO;
import org.xhy.application.rag.dto.UploadFileRequest;
import org.xhy.domain.rag.model.FileDetailEntity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件详情转换器
 * @author shilong.zang
 * @date 2024-12-09
 */
public class FileDetailAssembler {

    /**
     * 上传请求转换为实体
     * @param request 上传请求
     * @param userId 用户ID
     * @return 文件实体
     */
    public static FileDetailEntity toEntity(UploadFileRequest request, Long userId) {
        if (request == null) {
            return null;
        }
        FileDetailEntity entity = new FileDetailEntity();
        entity.setDataSetId(request.getDatasetId());
        entity.setMultipartFile(request.getFile());
        entity.setUserId(userId);
        return entity;
    }

    /**
     * 实体转换为DTO
     * @param entity 文件实体
     * @return 文件DTO
     */
    public static FileDetailDTO toDTO(FileDetailEntity entity) {
        if (entity == null) {
            return null;
        }
        FileDetailDTO dto = new FileDetailDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    /**
     * 实体列表转换为DTO列表
     * @param entities 文件实体列表
     * @return 文件DTO列表
     */
    public static List<FileDetailDTO> toDTOs(List<FileDetailEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(FileDetailAssembler::toDTO)
                .collect(Collectors.toList());
    }
}