package org.xhy.domain.llm.model;

import com.baomidou.mybatisplus.annotation.*;
import org.apache.ibatis.type.JdbcType;
import org.xhy.domain.llm.model.enums.ModelType;
import org.xhy.infrastructure.converter.ModelTypeConverter;
import org.xhy.infrastructure.entity.BaseEntity;
import org.xhy.infrastructure.exception.BusinessException;

/** 模型领域模型 */
@TableName("models")
public class ModelEntity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String userId;
    private String providerId;
    private String modelId;
    private String name;
    private String description;

    /** 模型部署名称 */
    private String modelEndpoint;

    private Boolean isOfficial;

    @TableField(typeHandler = ModelTypeConverter.class, jdbcType = JdbcType.VARCHAR)
    private ModelType type;

    private Boolean status;

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

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ModelType getType() {
        return type;
    }

    public void setType(ModelType type) {
        this.type = type;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Boolean getOfficial() {
        return isOfficial;
    }

    public void setOfficial(Boolean official) {
        isOfficial = official;
    }

    public String getModelEndpoint() {
        return modelEndpoint;
    }

    public void setModelEndpoint(String modelEndpoint) {
        this.modelEndpoint = modelEndpoint;
    }

    public void isActive() {
        if (!status) {
            throw new BusinessException("模型未激活");
        }
    }
}