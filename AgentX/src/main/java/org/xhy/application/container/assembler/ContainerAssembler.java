package org.xhy.application.container.assembler;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.xhy.application.container.dto.ContainerDTO;
import org.xhy.domain.container.model.ContainerEntity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** 容器装配器 */
public class ContainerAssembler {

    /** 将容器实体转换为DTO
     * 
     * @param entity 容器实体
     * @return 容器DTO */
    public static ContainerDTO toDTO(ContainerEntity entity) {
        if (entity == null) {
            return null;
        }

        ContainerDTO dto = new ContainerDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    /** 将容器实体列表转换为DTO列表
     * 
     * @param entities 容器实体列表
     * @return 容器DTO列表 */
    public static List<ContainerDTO> toDTOs(List<ContainerEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream().map(ContainerAssembler::toDTO).collect(Collectors.toList());
    }

    /** 将容器实体分页转换为DTO分页
     * 
     * @param entityPage 容器实体分页
     * @return 容器DTO分页 */
    public static Page<ContainerDTO> toDTOPage(Page<ContainerEntity> entityPage) {
        Page<ContainerDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());

        List<ContainerDTO> dtoList = toDTOs(entityPage.getRecords());
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }
}