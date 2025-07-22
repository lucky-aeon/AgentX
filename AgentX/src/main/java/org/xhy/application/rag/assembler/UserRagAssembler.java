package org.xhy.application.rag.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.rag.dto.UserRagDTO;
import org.xhy.domain.rag.model.RagQaDatasetEntity;
import org.xhy.domain.rag.model.UserRagEntity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** 用户RAG Assembler
 * @author xhy
 * @date 2025-07-16 <br/>
 */
public class UserRagAssembler {

    /** Convert Entity to DTO using BeanUtils */
    public static UserRagDTO toDTO(UserRagEntity entity) {
        if (entity == null) {
            return null;
        }

        UserRagDTO dto = new UserRagDTO();
        BeanUtils.copyProperties(entity, dto);

        return dto;
    }

    /** Convert Entity list to DTO list */
    public static List<UserRagDTO> toDTOs(List<UserRagEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(UserRagAssembler::toDTO).collect(Collectors.toList());
    }

    /** Enrich UserRagDTO with version info */
    public static UserRagDTO enrichWithVersionInfo(UserRagEntity entity, String originalRagId, Integer fileCount,
            Integer documentCount, String creatorNickname) {
        if (entity == null) {
            return null;
        }

        UserRagDTO dto = toDTO(entity);
        dto.setOriginalRagId(originalRagId);
        dto.setFileCount(fileCount);
        dto.setDocumentCount(documentCount);
        dto.setCreatorNickname(creatorNickname);

        return dto;
    }

    /** Enrich UserRagDTO with version info including creator ID */
    public static UserRagDTO enrichWithVersionInfo(UserRagEntity entity, String originalRagId, Integer fileCount,
            Integer documentCount, String creatorNickname, String creatorId) {
        if (entity == null) {
            return null;
        }

        UserRagDTO dto = toDTO(entity);
        dto.setOriginalRagId(originalRagId);
        dto.setFileCount(fileCount);
        dto.setDocumentCount(documentCount);
        dto.setCreatorNickname(creatorNickname);
        dto.setCreatorId(creatorId);

        return dto;
    }

    /** 处理REFERENCE类型：获取原始RAG的实时信息
     * 
     * @param entity 用户RAG实体
     * @param originalRag 原始RAG数据集
     * @param creatorNickname 创建者昵称
     * @return 丰富信息后的DTO */
    public static UserRagDTO enrichWithReferenceInfo(UserRagEntity entity, RagQaDatasetEntity originalRag, 
            String creatorNickname) {
        if (entity == null) {
            return null;
        }

        UserRagDTO dto = toDTO(entity);
        
        // 使用原始RAG的实时信息覆盖快照数据
        dto.setName(originalRag.getName());
        dto.setDescription(originalRag.getDescription());
        dto.setIcon(originalRag.getIcon());
        
        // 设置统计信息和创建者信息
        dto.setCreatorNickname(creatorNickname);
        dto.setCreatorId(originalRag.getUserId());
        
        return dto;
    }

    /** 处理SNAPSHOT类型：使用快照数据
     * 
     * @param entity 用户RAG实体
     * @param fileCount 文件数量（从用户快照统计）
     * @param documentCount 文档数量（从用户快照统计）
     * @param creatorNickname 创建者昵称
     * @param creatorId 创建者ID
     * @return 丰富信息后的DTO */
    public static UserRagDTO enrichWithSnapshotInfo(UserRagEntity entity, Integer fileCount, Integer documentCount, 
            String creatorNickname, String creatorId) {
        if (entity == null) {
            return null;
        }

        UserRagDTO dto = toDTO(entity);
        
        // 使用entity中的快照信息（name、description、icon已经是快照数据）
        // 只补充统计信息和创建者信息
        dto.setFileCount(fileCount);
        dto.setDocumentCount(documentCount);
        dto.setCreatorNickname(creatorNickname);
        dto.setCreatorId(creatorId);
        
        return dto;
    }
}