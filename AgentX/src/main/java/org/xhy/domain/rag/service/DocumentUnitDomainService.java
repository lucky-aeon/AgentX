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

import java.util.List;

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

    /** 根据文件id获取所有语料 */
    public List<DocumentUnitEntity> listDocumentUnitsByFileId(String fileId) {
        LambdaQueryWrapper<DocumentUnitEntity> wrapper = Wrappers.<DocumentUnitEntity>lambdaQuery()
                .eq(DocumentUnitEntity::getFileId, fileId).orderByAsc(DocumentUnitEntity::getPage);

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

    /** 根据文件ID查询向量化的文档单元列表
     * 
     * @param fileId 文件ID
     * @return 向量化的文档单元列表 */
    public List<DocumentUnitEntity> listVectorizedDocumentsByFile(String fileId) {
        LambdaQueryWrapper<DocumentUnitEntity> wrapper = Wrappers.<DocumentUnitEntity>lambdaQuery()
                .eq(DocumentUnitEntity::getFileId, fileId).eq(DocumentUnitEntity::getIsVector, true);
        return documentUnitRepository.selectList(wrapper);
    }

    /** 根据文件ID查询所有文档单元列表
     * 
     * @param fileId 文件ID
     * @return 文档单元列表 */
    public List<DocumentUnitEntity> listDocumentsByFile(String fileId) {
        LambdaQueryWrapper<DocumentUnitEntity> wrapper = Wrappers.<DocumentUnitEntity>lambdaQuery()
                .eq(DocumentUnitEntity::getFileId, fileId);
        return documentUnitRepository.selectList(wrapper);
    }

    /** 根据文件ID和OCR状态查询文档单元列表
     * 
     * @param fileId 文件ID
     * @param isOcr 是否已OCR
     * @param isVector 是否已向量化
     * @return 文档单元列表 */
    public List<DocumentUnitEntity> listDocumentsByFileAndStatus(String fileId, Boolean isOcr, Boolean isVector) {
        LambdaQueryWrapper<DocumentUnitEntity> wrapper = Wrappers.<DocumentUnitEntity>lambdaQuery()
                .eq(DocumentUnitEntity::getFileId, fileId);

        if (isOcr != null) {
            wrapper.eq(DocumentUnitEntity::getIsOcr, isOcr);
        }

        if (isVector != null) {
            wrapper.eq(DocumentUnitEntity::getIsVector, isVector);
        }

        return documentUnitRepository.selectList(wrapper);
    }

    /** 批量删除文档单元
     * 
     * @param documentUnitIds 文档单元ID列表 */
    public void batchDeleteDocumentUnits(List<String> documentUnitIds) {
        if (documentUnitIds != null && !documentUnitIds.isEmpty()) {
            documentUnitRepository.deleteByIds(documentUnitIds);
        }
    }

    /** 更新单个文档单元（包括向量化状态）
     * 
     * @param documentUnit 文档单元实体 */
    public void updateDocumentUnitById(DocumentUnitEntity documentUnit) {
        documentUnitRepository.updateById(documentUnit);
    }
}