package org.xhy.domain.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.infrastructure.entity.Operator;

/** 文档单元领域服务
 * 
 * @author shilong.zang */
@Service
public class DocumentUnitDomainService {

    private final DocumentUnitRepository documentUnitRepository;

    public DocumentUnitDomainService(DocumentUnitRepository documentUnitRepository) {
        this.documentUnitRepository = documentUnitRepository;
    }

    /** 分页查询文件的语料
     * 
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 页大小
     * @param keyword 搜索关键词
     * @return 分页结果 */
    public IPage<DocumentUnitEntity> listDocumentUnits(String fileId, String userId, Integer page, Integer pageSize,
            String keyword) {
        LambdaQueryWrapper<DocumentUnitEntity> wrapper = Wrappers.<DocumentUnitEntity>lambdaQuery()
                .eq(DocumentUnitEntity::getFileId, fileId);

        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            wrapper.like(DocumentUnitEntity::getContent, keyword);
        }

        // 按页码排序
        wrapper.orderByAsc(DocumentUnitEntity::getPage);

        Page<DocumentUnitEntity> pageParam = new Page<>(page, pageSize);
        return documentUnitRepository.selectPage(pageParam, wrapper);
    }

    /** 根据ID获取语料
     * 
     * @param documentUnitId 语料ID
     * @param userId 用户ID
     * @return 语料实体 */
    public DocumentUnitEntity getDocumentUnit(String documentUnitId, String userId) {
        DocumentUnitEntity entity = documentUnitRepository.selectById(documentUnitId);
        if (entity == null) {
            throw new IllegalArgumentException("语料不存在");
        }
        return entity;
    }

    /**
     * 根据文件id获取所有语料
     */
    public List<DocumentUnitEntity> listDocumentUnitsByFileId(String fileId) {
        LambdaQueryWrapper<DocumentUnitEntity> wrapper = Wrappers.<DocumentUnitEntity>lambdaQuery()
                .eq(DocumentUnitEntity::getFileId, fileId)
                .orderByAsc(DocumentUnitEntity::getPage);

        return documentUnitRepository.selectList(wrapper);
    }

    /** 更新语料内容
     * 
     * @param entity 语料实体
     * @param userId 用户ID */
    public void updateDocumentUnit(DocumentUnitEntity entity, String userId) {
        LambdaUpdateWrapper<DocumentUnitEntity> updateWrapper = Wrappers.<DocumentUnitEntity>lambdaUpdate()
                .eq(DocumentUnitEntity::getId, entity.getId()).set(DocumentUnitEntity::getContent, entity.getContent())
                .set(entity.getIsVector() != null, DocumentUnitEntity::getIsVector, entity.getIsVector());

        documentUnitRepository.checkedUpdate(entity, updateWrapper);
    }

    /** 删除语料
     * 
     * @param documentUnitId 语料ID
     * @param userId 用户ID */
    public void deleteDocumentUnit(String documentUnitId, String userId) {
        LambdaUpdateWrapper<DocumentUnitEntity> deleteWrapper = Wrappers.<DocumentUnitEntity>lambdaUpdate()
                .eq(DocumentUnitEntity::getId, documentUnitId);

        documentUnitRepository.checkedDelete(deleteWrapper);
    }

    /** 检查语料是否存在
     * 
     * @param documentUnitId 语料ID
     * @param userId 用户ID */
    public void checkDocumentUnitExists(String documentUnitId, String userId) {
        getDocumentUnit(documentUnitId, userId);
    }
}