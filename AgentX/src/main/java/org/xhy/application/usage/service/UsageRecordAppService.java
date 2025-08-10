package org.xhy.application.usage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.xhy.application.usage.assembler.UsageRecordAssembler;
import org.xhy.application.usage.dto.UsageRecordDTO;
import org.xhy.domain.user.model.UsageRecordEntity;
import org.xhy.domain.user.service.UsageRecordDomainService;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.interfaces.dto.usage.request.QueryUsageRecordRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 使用记录应用服务 处理使用记录相关的业务流程编排 */
@Service
public class UsageRecordAppService {

    private final UsageRecordDomainService usageRecordDomainService;

    public UsageRecordAppService(UsageRecordDomainService usageRecordDomainService) {
        this.usageRecordDomainService = usageRecordDomainService;
    }

    /** 根据ID获取使用记录
     * @param recordId 记录ID
     * @return 使用记录DTO */
    public UsageRecordDTO getUsageRecordById(String recordId) {
        UsageRecordEntity entity = usageRecordDomainService.getUsageRecordById(recordId);
        if (entity == null) {
            throw new BusinessException("使用记录不存在");
        }
        return UsageRecordAssembler.toDTO(entity);
    }

    /** 获取用户的使用记录（分页）
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 使用记录分页结果 */
    public Page<UsageRecordDTO> getUserUsageRecords(String userId, int page, int size) {
        Page<UsageRecordEntity> entityPage = usageRecordDomainService.getUserUsageHistory(userId, page, size);

        List<UsageRecordDTO> dtoList = UsageRecordAssembler.toDTOs(entityPage.getRecords());

        Page<UsageRecordDTO> resultPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(),
                entityPage.getTotal());
        resultPage.setRecords(dtoList);

        return resultPage;
    }

    /** 按条件查询使用记录
     * @param request 查询请求
     * @return 使用记录分页结果 */
    public Page<UsageRecordDTO> queryUsageRecords(QueryUsageRecordRequest request) {
        // 构建查询条件

        Page<UsageRecordEntity> usageRecordEntityPage = usageRecordDomainService.queryUsageRecords(request);

        // 转换结果
        List<UsageRecordDTO> dtoList = UsageRecordAssembler.toDTOs(usageRecordEntityPage.getRecords());

        Page<UsageRecordDTO> resultPage = new Page<>(usageRecordEntityPage.getCurrent(),
                usageRecordEntityPage.getSize(), usageRecordEntityPage.getTotal());
        resultPage.setRecords(dtoList);

        return resultPage;
    }

    /** 获取用户某个商品的使用记录
     * @param userId 用户ID
     * @param productId 商品ID
     * @param page 页码
     * @param size 每页大小
     * @return 使用记录分页结果 */
    public Page<UsageRecordDTO> getUserProductUsageRecords(String userId, String productId, int page, int size) {
        Page<UsageRecordEntity> entityPage = usageRecordDomainService.getUserProductUsageHistory(userId, productId,
                page, size);

        List<UsageRecordDTO> dtoList = UsageRecordAssembler.toDTOs(entityPage.getRecords());

        Page<UsageRecordDTO> resultPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(),
                entityPage.getTotal());
        resultPage.setRecords(dtoList);

        return resultPage;
    }

    /** 获取用户在指定时间范围内的使用记录
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 使用记录列表 */
    public List<UsageRecordDTO> getUserUsageByTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        List<UsageRecordEntity> entities = usageRecordDomainService.getUserUsageByTimeRange(userId, startTime, endTime);
        return UsageRecordAssembler.toDTOs(entities);
    }

    /** 获取商品的使用记录（分页）
     * @param productId 商品ID
     * @param page 页码
     * @param size 每页大小
     * @return 使用记录分页结果 */
    public Page<UsageRecordDTO> getProductUsageRecords(String productId, int page, int size) {
        Page<UsageRecordEntity> entityPage = usageRecordDomainService.getProductUsageHistory(productId, page, size);

        List<UsageRecordDTO> dtoList = UsageRecordAssembler.toDTOs(entityPage.getRecords());

        Page<UsageRecordDTO> resultPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(),
                entityPage.getTotal());
        resultPage.setRecords(dtoList);

        return resultPage;
    }

    /** 检查请求ID是否已存在
     * @param requestId 请求ID
     * @return 是否存在 */
    public boolean existsByRequestId(String requestId) {
        return usageRecordDomainService.existsByRequestId(requestId);
    }

    /** 统计用户的总消费金额
     * @param userId 用户ID
     * @return 总消费金额 */
    public BigDecimal getUserTotalCost(String userId) {
        Page<UsageRecordEntity> entityPage = usageRecordDomainService.getUserUsageHistory(userId, 1, Integer.MAX_VALUE);

        return entityPage.getRecords().stream().map(UsageRecordEntity::getCost).reduce(BigDecimal.ZERO,
                BigDecimal::add);
    }

    /** 统计用户在指定时间范围内的消费金额
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 消费金额 */
    public BigDecimal getUserCostByTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        List<UsageRecordEntity> entities = usageRecordDomainService.getUserUsageByTimeRange(userId, startTime, endTime);

        return entities.stream().map(UsageRecordEntity::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}