package org.xhy.domain.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.xhy.domain.user.model.UsageRecordEntity;
import org.xhy.domain.user.repository.UsageRecordRepository;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.interfaces.dto.usage.request.QueryUsageRecordRequest;

import java.time.LocalDateTime;
import java.util.List;

/** 用量记录领域服务 处理用量记录相关的核心业务逻辑 */
@Service
public class UsageRecordDomainService {

    private final UsageRecordRepository usageRecordRepository;

    public UsageRecordDomainService(UsageRecordRepository usageRecordRepository) {
        this.usageRecordRepository = usageRecordRepository;
    }

    /** 获取使用记录仓储（供应用层使用）
     * @return 使用记录仓储 */
    public UsageRecordRepository getUsageRecordRepository() {
        return usageRecordRepository;
    }

    /** 记录用量
     * @param record 用量记录实体
     * @return 保存后的用量记录实体 */
    public UsageRecordEntity recordUsage(UsageRecordEntity record) {
        // 验证记录信息
        record.validate();

        // 检查幂等性
        if (checkDuplicateRequest(record.getRequestId())) {
            throw new BusinessException("重复的请求ID: " + record.getRequestId());
        }

        usageRecordRepository.insert(record);
        return record;
    }

    /** 检查请求是否重复（幂等性检查）
     * @param requestId 请求ID
     * @return 是否重复 */
    public boolean checkDuplicateRequest(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return false;
        }

        LambdaQueryWrapper<UsageRecordEntity> wrapper = Wrappers.<UsageRecordEntity>lambdaQuery()
                .eq(UsageRecordEntity::getRequestId, requestId);

        return usageRecordRepository.exists(wrapper);
    }

    /** 检查请求ID是否存在（用于幂等性检查）
     * @param requestId 请求ID
     * @return 是否存在 */
    public boolean existsByRequestId(String requestId) {
        return checkDuplicateRequest(requestId);
    }

    /** 创建用量记录
     * @param record 用量记录实体
     * @return 保存后的用量记录实体 */
    public UsageRecordEntity createUsageRecord(UsageRecordEntity record) {
        return recordUsage(record);
    }

    /** 获取用户的用量历史（分页）
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 用量记录分页结果 */
    public Page<UsageRecordEntity> getUserUsageHistory(String userId, int pageNum, int pageSize) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new BusinessException("用户ID不能为空");
        }

        LambdaQueryWrapper<UsageRecordEntity> wrapper = Wrappers.<UsageRecordEntity>lambdaQuery()
                .eq(UsageRecordEntity::getUserId, userId).orderByDesc(UsageRecordEntity::getBilledAt);

        Page<UsageRecordEntity> page = new Page<>(pageNum, pageSize);
        return usageRecordRepository.selectPage(page, wrapper);
    }

    /** 获取用户在指定时间范围内的用量记录
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 用量记录列表 */
    public List<UsageRecordEntity> getUserUsageByTimeRange(String userId, LocalDateTime startTime,
            LocalDateTime endTime) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new BusinessException("用户ID不能为空");
        }
        if (startTime == null || endTime == null) {
            throw new BusinessException("时间范围不能为空");
        }
        if (startTime.isAfter(endTime)) {
            throw new BusinessException("开始时间不能晚于结束时间");
        }

        LambdaQueryWrapper<UsageRecordEntity> wrapper = Wrappers.<UsageRecordEntity>lambdaQuery()
                .eq(UsageRecordEntity::getUserId, userId).ge(UsageRecordEntity::getBilledAt, startTime)
                .le(UsageRecordEntity::getBilledAt, endTime).orderByDesc(UsageRecordEntity::getBilledAt);

        return usageRecordRepository.selectList(wrapper);
    }

    /** 获取商品的用量记录（分页）
     * @param productId 商品ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 用量记录分页结果 */
    public Page<UsageRecordEntity> getProductUsageHistory(String productId, int pageNum, int pageSize) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new BusinessException("商品ID不能为空");
        }

        LambdaQueryWrapper<UsageRecordEntity> wrapper = Wrappers.<UsageRecordEntity>lambdaQuery()
                .eq(UsageRecordEntity::getProductId, productId).orderByDesc(UsageRecordEntity::getBilledAt);

        Page<UsageRecordEntity> page = new Page<>(pageNum, pageSize);
        return usageRecordRepository.selectPage(page, wrapper);
    }

    /** 获取用户使用特定商品的用量记录（分页）
     * @param userId 用户ID
     * @param productId 商品ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 用量记录分页结果 */
    public Page<UsageRecordEntity> getUserProductUsageHistory(String userId, String productId, int pageNum,
            int pageSize) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new BusinessException("用户ID不能为空");
        }
        if (productId == null || productId.trim().isEmpty()) {
            throw new BusinessException("商品ID不能为空");
        }

        LambdaQueryWrapper<UsageRecordEntity> wrapper = Wrappers.<UsageRecordEntity>lambdaQuery()
                .eq(UsageRecordEntity::getUserId, userId).eq(UsageRecordEntity::getProductId, productId)
                .orderByDesc(UsageRecordEntity::getBilledAt);

        Page<UsageRecordEntity> page = new Page<>(pageNum, pageSize);
        return usageRecordRepository.selectPage(page, wrapper);
    }

    /** 根据ID获取用量记录
     * @param recordId 记录ID
     * @return 用量记录实体，如果不存在则返回null */
    public UsageRecordEntity getUsageRecordById(String recordId) {
        if (recordId == null || recordId.trim().isEmpty()) {
            return null;
        }
        return usageRecordRepository.selectById(recordId);
    }

    /** 删除用量记录（软删除）
     * @param recordId 记录ID */
    public void deleteUsageRecord(String recordId) {
        UsageRecordEntity record = getUsageRecordById(recordId);
        if (record == null) {
            throw new BusinessException("用量记录不存在");
        }

        usageRecordRepository.deleteById(recordId);
    }

    /** 批量记录用量
     * @param records 用量记录列表 */
    public void batchRecordUsage(List<UsageRecordEntity> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        // 验证所有记录
        for (UsageRecordEntity record : records) {
            record.validate();
            if (checkDuplicateRequest(record.getRequestId())) {
                throw new BusinessException("重复的请求ID: " + record.getRequestId());
            }
        }

        // 批量插入
        for (UsageRecordEntity record : records) {
            usageRecordRepository.insert(record);
        }
    }

    public Page<UsageRecordEntity> queryUsageRecords(QueryUsageRecordRequest request) {
        LambdaQueryWrapper<UsageRecordEntity> wrapper = Wrappers.<UsageRecordEntity>lambdaQuery()
                .eq(StringUtils.isNotBlank(request.getUserId()), UsageRecordEntity::getUserId, request.getUserId())
                .eq(StringUtils.isNotBlank(request.getProductId()), UsageRecordEntity::getProductId,
                        request.getProductId())
                .eq(StringUtils.isNotBlank(request.getRequestId()), UsageRecordEntity::getRequestId,
                        request.getRequestId())
                .ge(request.getStartTime() != null, UsageRecordEntity::getBilledAt, request.getStartTime())
                .le(request.getEndTime() != null, UsageRecordEntity::getBilledAt, request.getEndTime())
                .orderByDesc(UsageRecordEntity::getBilledAt);

        return usageRecordRepository.selectPage(new Page<>(request.getPage(), request.getPageSize()), wrapper);
    }
}