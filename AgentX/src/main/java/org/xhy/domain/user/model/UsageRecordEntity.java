package org.xhy.domain.user.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.xhy.infrastructure.converter.QuantityDataConverter;
import org.xhy.infrastructure.entity.BaseEntity;
import org.xhy.infrastructure.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/** 用量记录实体 记录每一次用量事件，用于审计和生成账单 */
@TableName(value = "usage_records", autoResultMap = true)
public class UsageRecordEntity extends BaseEntity {

    /** 记录ID */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 用户ID */
    @TableField("user_id")
    private String userId;

    /** 关联的商品ID */
    @TableField("product_id")
    private String productId;

    /** 用量数据 JSON格式 */
    @TableField(value = "quantity_data", typeHandler = QuantityDataConverter.class)
    private Map<String, Object> quantityData;

    /** 本次用量产生的费用 */
    @TableField("cost")
    private BigDecimal cost;

    /** 原始请求ID，用于幂等性校验 */
    @TableField("request_id")
    private String requestId;

    /** 计费发生时间 */
    @TableField("billed_at")
    private LocalDateTime billedAt;

    /** 业务服务名称 （如：GPT-4 模型调用） */
    @TableField("service_name")
    private String serviceName;

    /** 服务类型 （如：模型服务） */
    @TableField("service_type")
    private String serviceType;

    /** 服务描述 */
    @TableField("service_description")
    private String serviceDescription;

    /** 定价规则说明 （如：输入 ¥0.002/1K tokens，输出 ¥0.006/1K tokens） */
    @TableField("pricing_rule")
    private String pricingRule;

    /** 关联实体名称 （如：具体的模型名称或Agent名称） */
    @TableField("related_entity_name")
    private String relatedEntityName;

    public UsageRecordEntity() {
        this.cost = BigDecimal.ZERO;
        this.billedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Map<String, Object> getQuantityData() {
        return quantityData;
    }

    public void setQuantityData(Map<String, Object> quantityData) {
        this.quantityData = quantityData;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public LocalDateTime getBilledAt() {
        return billedAt;
    }

    public void setBilledAt(LocalDateTime billedAt) {
        this.billedAt = billedAt;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    public String getPricingRule() {
        return pricingRule;
    }

    public void setPricingRule(String pricingRule) {
        this.pricingRule = pricingRule;
    }

    public String getRelatedEntityName() {
        return relatedEntityName;
    }

    public void setRelatedEntityName(String relatedEntityName) {
        this.relatedEntityName = relatedEntityName;
    }

    /** 验证记录信息 */
    public void validate() {
        if (userId == null || userId.trim().isEmpty()) {
            throw new BusinessException("用户ID不能为空");
        }
        if (productId == null || productId.trim().isEmpty()) {
            throw new BusinessException("商品ID不能为空");
        }
        if (quantityData == null || quantityData.isEmpty()) {
            throw new BusinessException("用量数据不能为空");
        }
        if (cost == null || cost.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("费用必须大于等于0");
        }
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new BusinessException("请求ID不能为空");
        }
        if (billedAt == null) {
            this.billedAt = LocalDateTime.now();
        }
    }

    /** 检查记录是否有效 */
    public boolean isValidRecord() {
        try {
            validate();
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    /** 获取用量数据中的指定字段
     * @param key 字段名
     * @return 字段值 */
    public Object getQuantityValue(String key) {
        if (quantityData == null) {
            return null;
        }
        return quantityData.get(key);
    }

    /** 获取用量数据中的整数值
     * @param key 字段名
     * @return 整数值，如果不存在或无法转换则返回0 */
    public Integer getQuantityIntValue(String key) {
        Object value = getQuantityValue(key);
        if (value == null) {
            return 0;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 获取用量数据中的长整数值
     * @param key 字段名
     * @return 长整数值，如果不存在或无法转换则返回0 */
    public Long getQuantityLongValue(String key) {
        Object value = getQuantityValue(key);
        if (value == null) {
            return 0L;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** 设置用量数据字段
     * @param key 字段名
     * @param value 字段值 */
    public void setQuantityValue(String key, Object value) {
        if (quantityData == null) {
            throw new BusinessException("用量数据未初始化");
        }
        quantityData.put(key, value);
    }

    /** 创建新的用量记录
     * @param userId 用户ID
     * @param productId 商品ID
     * @param quantityData 用量数据
     * @param cost 费用
     * @param requestId 请求ID
     * @return 用量记录实体 */
    public static UsageRecordEntity createNew(String userId, String productId, Map<String, Object> quantityData,
            BigDecimal cost, String requestId) {
        UsageRecordEntity record = new UsageRecordEntity();
        record.setUserId(userId);
        record.setProductId(productId);
        record.setQuantityData(quantityData);
        record.setCost(cost);
        record.setRequestId(requestId);
        record.validate();
        return record;
    }

    /** 创建新的用量记录（包含业务信息）
     * @param userId 用户ID
     * @param productId 商品ID
     * @param quantityData 用量数据
     * @param cost 费用
     * @param requestId 请求ID
     * @param serviceName 服务名称
     * @param serviceType 服务类型
     * @param serviceDescription 服务描述
     * @param pricingRule 定价规则
     * @param relatedEntityName 关联实体名称
     * @return 用量记录实体 */
    public static UsageRecordEntity createNewWithBusinessInfo(String userId, String productId,
            Map<String, Object> quantityData, BigDecimal cost, String requestId, String serviceName, String serviceType,
            String serviceDescription, String pricingRule, String relatedEntityName) {
        UsageRecordEntity record = new UsageRecordEntity();
        record.setUserId(userId);
        record.setProductId(productId);
        record.setQuantityData(quantityData);
        record.setCost(cost);
        record.setRequestId(requestId);
        record.setServiceName(serviceName);
        record.setServiceType(serviceType);
        record.setServiceDescription(serviceDescription);
        record.setPricingRule(pricingRule);
        record.setRelatedEntityName(relatedEntityName);
        record.validate();
        return record;
    }
}