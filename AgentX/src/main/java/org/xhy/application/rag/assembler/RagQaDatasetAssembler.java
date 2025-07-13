package org.xhy.application.rag.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.rag.dto.CreateDatasetRequest;
import org.xhy.application.rag.dto.RagQaDatasetDTO;
import org.xhy.application.rag.dto.UpdateDatasetRequest;
import org.xhy.domain.rag.model.RagQaDatasetEntity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** RAG数据集转换器
 * @author shilong.zang
 * @date 2024-12-09 */
public class RagQaDatasetAssembler {

    /** 创建请求转换为实体
     * @param request 创建请求
     * @param userId 用户ID
     * @return 数据集实体 */
    public static RagQaDatasetEntity toEntity(CreateDatasetRequest request, String userId) {
        if (request == null) {
            return null;
        }
        RagQaDatasetEntity entity = new RagQaDatasetEntity();
        BeanUtils.copyProperties(request, entity);
        entity.setUserId(userId);
        return entity;
    }

    /** 更新请求转换为实体
     * @param request 更新请求
     * @param datasetId 数据集ID
     * @param userId 用户ID
     * @return 数据集实体 */
    public static RagQaDatasetEntity toEntity(UpdateDatasetRequest request, String datasetId, String userId) {
        if (request == null) {
            return null;
        }
        RagQaDatasetEntity entity = new RagQaDatasetEntity();
        BeanUtils.copyProperties(request, entity);
        entity.setId(datasetId);
        entity.setUserId(userId);
        return entity;
    }

    /** 实体转换为DTO
     * @param entity 数据集实体
     * @return 数据集DTO */
    public static RagQaDatasetDTO toDTO(RagQaDatasetEntity entity) {
        if (entity == null) {
            return null;
        }
        RagQaDatasetDTO dto = new RagQaDatasetDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    /** 实体转换为DTO，包含文件数量
     * @param entity 数据集实体
     * @param fileCount 文件数量
     * @return 数据集DTO */
    public static RagQaDatasetDTO toDTO(RagQaDatasetEntity entity, Long fileCount) {
        if (entity == null) {
            return null;
        }
        RagQaDatasetDTO dto = new RagQaDatasetDTO();
        BeanUtils.copyProperties(entity, dto);
        dto.setFileCount(fileCount);
        return dto;
    }

    /** 实体列表转换为DTO列表
     * @param entities 数据集实体列表
     * @return 数据集DTO列表 */
    public static List<RagQaDatasetDTO> toDTOs(List<RagQaDatasetEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(RagQaDatasetAssembler::toDTO).collect(Collectors.toList());
    }
}