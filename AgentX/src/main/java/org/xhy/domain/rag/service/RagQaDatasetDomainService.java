package org.xhy.domain.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.model.RagQaDatasetEntity;
import org.xhy.domain.rag.repository.RagQaDatasetRepository;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.List;

/** RAG知识库数据集领域服务
 * @author shilong.zang
 * @date 17:43 <br/>
 */
@Service
public class RagQaDatasetDomainService {

    private final RagQaDatasetRepository ragQaDatasetRepository;

    public RagQaDatasetDomainService(RagQaDatasetRepository ragQaDatasetRepository) {
        this.ragQaDatasetRepository = ragQaDatasetRepository;
    }

    /** 创建数据集
     * @param dataset 数据集实体
     * @return 创建后的数据集实体 */
    public RagQaDatasetEntity createDataset(RagQaDatasetEntity dataset) {
        validateDatasetName(dataset.getName(), dataset.getUserId());
        ragQaDatasetRepository.insert(dataset);
        return dataset;
    }

    /** 根据ID获取数据集
     * @param datasetId 数据集ID
     * @param userId 用户ID
     * @return 数据集实体 */
    public RagQaDatasetEntity getDataset(String datasetId, String userId) {
        LambdaQueryWrapper<RagQaDatasetEntity> wrapper = Wrappers.<RagQaDatasetEntity>lambdaQuery()
                .eq(RagQaDatasetEntity::getId, datasetId).eq(RagQaDatasetEntity::getUserId, userId);
        RagQaDatasetEntity dataset = ragQaDatasetRepository.selectOne(wrapper);
        if (dataset == null) {
            throw new BusinessException("数据集不存在");
        }
        return dataset;
    }

    /** 查找数据集（可返回null）
     * @param datasetId 数据集ID
     * @param userId 用户ID
     * @return 数据集实体或null */
    public RagQaDatasetEntity findDataset(String datasetId, String userId) {
        LambdaQueryWrapper<RagQaDatasetEntity> wrapper = Wrappers.<RagQaDatasetEntity>lambdaQuery()
                .eq(RagQaDatasetEntity::getId, datasetId).eq(RagQaDatasetEntity::getUserId, userId);
        return ragQaDatasetRepository.selectOne(wrapper);
    }

    /** 检查数据集是否存在
     * @param datasetId 数据集ID
     * @param userId 用户ID
     * @return 是否存在 */
    public boolean existsDataset(String datasetId, String userId) {
        LambdaQueryWrapper<RagQaDatasetEntity> wrapper = Wrappers.<RagQaDatasetEntity>lambdaQuery()
                .eq(RagQaDatasetEntity::getId, datasetId).eq(RagQaDatasetEntity::getUserId, userId);
        return ragQaDatasetRepository.exists(wrapper);
    }

    /** 检查数据集存在性，不存在则抛出异常
     * @param datasetId 数据集ID
     * @param userId 用户ID */
    public void checkDatasetExists(String datasetId, String userId) {
        if (!existsDataset(datasetId, userId)) {
            throw new BusinessException("数据集不存在");
        }
    }

    /** 更新数据集
     * @param dataset 数据集实体 */
    public void updateDataset(RagQaDatasetEntity dataset) {
        validateDatasetName(dataset.getName(), dataset.getUserId(), dataset.getId());
        LambdaUpdateWrapper<RagQaDatasetEntity> wrapper = Wrappers.<RagQaDatasetEntity>lambdaUpdate()
                .eq(RagQaDatasetEntity::getId, dataset.getId())
                .eq(dataset.needCheckUserId(), RagQaDatasetEntity::getUserId, dataset.getUserId());
        ragQaDatasetRepository.checkedUpdate(dataset, wrapper);
    }

    /** 删除数据集
     * @param datasetId 数据集ID
     * @param userId 用户ID */
    public void deleteDataset(String datasetId, String userId) {
        LambdaUpdateWrapper<RagQaDatasetEntity> wrapper = Wrappers.<RagQaDatasetEntity>lambdaUpdate()
                .eq(RagQaDatasetEntity::getId, datasetId).eq(RagQaDatasetEntity::getUserId, userId);
        ragQaDatasetRepository.checkedDelete(wrapper);
    }

    /** 分页查询用户的数据集
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词
     * @return 分页结果 */
    public IPage<RagQaDatasetEntity> listDatasets(String userId, Integer page, Integer pageSize, String keyword) {
        LambdaQueryWrapper<RagQaDatasetEntity> wrapper = Wrappers.<RagQaDatasetEntity>lambdaQuery()
                .eq(RagQaDatasetEntity::getUserId, userId);

        // 关键词搜索
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(RagQaDatasetEntity::getName, keyword).or().like(RagQaDatasetEntity::getDescription,
                    keyword));
        }

        wrapper.orderByDesc(RagQaDatasetEntity::getCreatedAt);

        Page<RagQaDatasetEntity> pageObj = new Page<>(page, pageSize);
        return ragQaDatasetRepository.selectPage(pageObj, wrapper);
    }

    /** 获取用户的所有数据集
     * @param userId 用户ID
     * @return 数据集列表 */
    public List<RagQaDatasetEntity> listAllDatasets(String userId) {
        LambdaQueryWrapper<RagQaDatasetEntity> wrapper = Wrappers.<RagQaDatasetEntity>lambdaQuery()
                .eq(RagQaDatasetEntity::getUserId, userId).orderByDesc(RagQaDatasetEntity::getCreatedAt);
        return ragQaDatasetRepository.selectList(wrapper);
    }

    /** 校验数据集名称唯一性
     * @param name 数据集名称
     * @param userId 用户ID */
    private void validateDatasetName(String name, String userId) {
        validateDatasetName(name, userId, null);
    }

    /** 校验数据集名称唯一性
     * @param name 数据集名称
     * @param userId 用户ID
     * @param excludeId 排除的数据集ID（更新时使用） */
    private void validateDatasetName(String name, String userId, String excludeId) {
        if (StringUtils.isBlank(name)) {
            throw new BusinessException("数据集名称不能为空");
        }

        LambdaQueryWrapper<RagQaDatasetEntity> wrapper = Wrappers.<RagQaDatasetEntity>lambdaQuery()
                .eq(RagQaDatasetEntity::getName, name).eq(RagQaDatasetEntity::getUserId, userId);

        if (StringUtils.isNotBlank(excludeId)) {
            wrapper.ne(RagQaDatasetEntity::getId, excludeId);
        }

        if (ragQaDatasetRepository.exists(wrapper)) {
            throw new BusinessException("数据集名称已存在");
        }
    }
}
