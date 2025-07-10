package org.xhy.application.rag.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.rag.dto.DocumentUnitDTO;
import org.xhy.domain.rag.model.DocumentUnitEntity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档单元转换器
 * 
 * @author shilong.zang
 */
public class DocumentUnitAssembler {
    
    /** Convert Entity to DTO using BeanUtils */
    public static DocumentUnitDTO toDTO(DocumentUnitEntity entity) {
        if (entity == null) {
            return null;
        }
        DocumentUnitDTO dto = new DocumentUnitDTO();
        BeanUtils.copyProperties(entity, dto);
        
        // 时间格式化
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().toString());
        }
        if (entity.getUpdatedAt() != null) {
            dto.setUpdatedAt(entity.getUpdatedAt().toString());
        }
        
        return dto;
    }
    
    /** Convert Entity list to DTO list */
    public static List<DocumentUnitDTO> toDTOs(List<DocumentUnitEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(DocumentUnitAssembler::toDTO).collect(Collectors.toList());
    }
}