package org.xhy.application.usage.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.usage.dto.UsageRecordDTO;
import org.xhy.domain.user.model.UsageRecordEntity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** 使用记录装配器 处理Entity、DTO之间的转换 */
public class UsageRecordAssembler {

    /** 将Entity转换为DTO
     * @param entity 使用记录实体
     * @return 使用记录DTO */
    public static UsageRecordDTO toDTO(UsageRecordEntity entity) {
        if (entity == null) {
            return null;
        }

        UsageRecordDTO dto = new UsageRecordDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    /** 将Entity列表转换为DTO列表
     * @param entities 使用记录实体列表
     * @return 使用记录DTO列表 */
    public static List<UsageRecordDTO> toDTOs(List<UsageRecordEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(UsageRecordAssembler::toDTO).collect(Collectors.toList());
    }
}