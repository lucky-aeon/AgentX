package org.xhy.application.rag.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.rag.dto.UserRagDTO;
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
    public static UserRagDTO enrichWithVersionInfo(UserRagEntity entity, 
                                                    String originalRagId,
                                                    Integer fileCount,
                                                    Integer documentCount,
                                                    String creatorNickname) {
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
}