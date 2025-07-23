package org.xhy.application.rag.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.rag.dto.CreateDatasetRequest;
import org.xhy.application.rag.dto.RagQaDatasetDTO;
import org.xhy.application.rag.dto.UpdateDatasetRequest;
import org.xhy.domain.rag.model.RagQaDatasetEntity;
import org.xhy.domain.rag.model.UserRagEntity;

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

    /** 用户安装的RAG转换为数据集DTO
     * @param userRag 用户安装的RAG实体
     * @param fileCount 文件数量
     * @return 数据集DTO */
    public static RagQaDatasetDTO fromUserRagEntity(UserRagEntity userRag, Long fileCount) {
        if (userRag == null) {
            return null;
        }
        RagQaDatasetDTO dto = new RagQaDatasetDTO();

        // 使用原始RAG ID作为数据集ID，这样Agent可以正确引用
        dto.setId(userRag.getOriginalRagId());

        // 设置用户RAG安装记录ID，用于调用已安装RAG相关接口
        dto.setUserRagId(userRag.getId());

        // 使用安装时的快照信息
        dto.setName(userRag.getName());
        dto.setDescription(userRag.getDescription());
        dto.setIcon(userRag.getIcon());

        // 文件数量
        dto.setFileCount(fileCount);

        // 时间信息使用安装时间
        dto.setCreatedAt(userRag.getInstalledAt());
        dto.setUpdatedAt(userRag.getUpdatedAt());

        // 用户ID保持一致
        dto.setUserId(userRag.getUserId());

        return dto;
    }

    /** 用户安装的RAG列表转换为数据集DTO列表
     * @param userRags 用户安装的RAG实体列表
     * @return 数据集DTO列表 */
    public static List<RagQaDatasetDTO> fromUserRagEntities(List<UserRagEntity> userRags) {
        if (userRags == null || userRags.isEmpty()) {
            return Collections.emptyList();
        }
        return userRags.stream().map(userRag -> fromUserRagEntity(userRag, 0L)).collect(Collectors.toList());
    }
}