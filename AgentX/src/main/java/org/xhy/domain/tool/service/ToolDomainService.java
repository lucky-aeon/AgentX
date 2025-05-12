package org.xhy.domain.tool.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.model.ToolVersionEntity;
import org.xhy.domain.tool.repository.ToolRepository;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.List;

/**
 * 工具领域服务
 */
@Service
public class ToolDomainService {

    private final ToolRepository toolRepository;
    private final ToolVersionRepository toolVersionRepository;

    public ToolDomainService(ToolRepository toolRepository, ToolVersionRepository toolVersionRepository) {
        this.toolRepository = toolRepository;
        this.toolVersionRepository = toolVersionRepository;
    }

    /**
     * 创建工具
     *
     * @param toolEntity 工具实体
     * @return 创建后的工具实体
     */
    @Transactional
    public ToolEntity createTool(ToolEntity toolEntity) {
        toolRepository.checkInsert(toolEntity);
        return toolEntity;
    }

    public ToolEntity getTool(String toolId, String userId) {
        Wrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaQuery()
                .eq(ToolEntity::getId, toolId)
                .eq(ToolEntity::getUserId, userId);
        ToolEntity toolEntity = toolRepository.selectOne(wrapper);
        if (toolEntity == null) {
            throw new BusinessException("工具不存在: " + toolId);
        }
        return toolEntity;
    }

    public List<ToolEntity> getUserTools(String userId) {
        LambdaQueryWrapper<ToolEntity> queryWrapper = Wrappers.<ToolEntity>lambdaQuery()
                .eq(ToolEntity::getUserId, userId)
                .orderByDesc(ToolEntity::getUpdatedAt);
        return toolRepository.selectList(queryWrapper);
    }

    public ToolEntity updateTool(ToolEntity toolEntity) {
        /**
         * todo xhy
         * 修改 namedescriptionicon labels触发 人工审核 状态
         * 修改 upload_urlupload_command触发整个状态扭转
         */
        toolRepository.checkedUpdateById(toolEntity);
        return toolEntity;
    }

    @Transactional
    public void deleteTool(String toolId, String userId) {
        // 删除工具
        Wrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaQuery()
                .eq(ToolEntity::getId, toolId)
                .eq(ToolEntity::getUserId, userId);

        // 删除工具版本
        Wrapper<ToolVersionEntity> versionWrapper = Wrappers.<ToolVersionEntity>lambdaQuery()
                .eq(ToolVersionEntity::getToolId, toolId);
        toolRepository.checkedDelete(wrapper);
        toolVersionRepository.delete(versionWrapper);

    }

    public void marketTool(String toolId, String userId) {
        ToolEntity toolEntity = getTool(toolId, userId);
        // 必须是审核通过才能上架
        if (toolEntity.getStatus() != ToolStatus.APPROVED) {
            throw new BusinessException("工具未审核通过，不能上架");
        }
        // 创建工具版本进行上架
        ToolVersionEntity toolVersionEntity = new ToolVersionEntity();
        BeanUtils.copyProperties(toolEntity, toolVersionEntity);
        toolVersionRepository.insert(toolVersionEntity);
    }
}