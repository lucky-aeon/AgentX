package org.xhy.application.container.assembler;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.xhy.application.container.dto.ContainerTemplateDTO;
import org.xhy.interfaces.dto.container.request.CreateContainerTemplateRequest;
import org.xhy.interfaces.dto.container.request.UpdateContainerTemplateRequest;
import org.xhy.domain.container.model.ContainerTemplateEntity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** 容器模板装配器 */
public class ContainerTemplateAssembler {

    /** 将创建请求转换为实体
     * 
     * @param request 创建请求
     * @param userId 创建者用户ID
     * @return 模板实体 */
    public static ContainerTemplateEntity toEntity(CreateContainerTemplateRequest request, String userId) {
        if (request == null) {
            return null;
        }

        ContainerTemplateEntity entity = new ContainerTemplateEntity();
        BeanUtils.copyProperties(request, entity);
        entity.setCreatedBy(userId);

        return entity;
    }

    /** 将更新请求转换为实体
     * 
     * @param request 更新请求
     * @return 模板实体 */
    public static ContainerTemplateEntity toEntity(UpdateContainerTemplateRequest request) {
        if (request == null) {
            return null;
        }

        ContainerTemplateEntity entity = new ContainerTemplateEntity();
        BeanUtils.copyProperties(request, entity);

        return entity;
    }

    /** 将实体转换为DTO
     * 
     * @param entity 模板实体
     * @return 模板DTO */
    public static ContainerTemplateDTO toDTO(ContainerTemplateEntity entity) {
        if (entity == null) {
            return null;
        }

        ContainerTemplateDTO dto = new ContainerTemplateDTO();
        BeanUtils.copyProperties(entity, dto);

        // 设置完整镜像名称
        dto.setFullImageName(entity.getFullImageName());

        return dto;
    }

    /** 将实体列表转换为DTO列表
     * 
     * @param entities 实体列表
     * @return DTO列表 */
    public static List<ContainerTemplateDTO> toDTOs(List<ContainerTemplateEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream().map(ContainerTemplateAssembler::toDTO).collect(Collectors.toList());
    }

    /** 将实体分页结果转换为DTO分页结果
     * 
     * @param entityPage 实体分页结果
     * @return DTO分页结果 */
    public static Page<ContainerTemplateDTO> toDTOPage(Page<ContainerTemplateEntity> entityPage) {
        if (entityPage == null) {
            return null;
        }

        Page<ContainerTemplateDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(),
                entityPage.getTotal());

        List<ContainerTemplateDTO> dtoList = toDTOs(entityPage.getRecords());
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }
}