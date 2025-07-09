package org.xhy.domain.rag.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import org.xhy.infrastructure.entity.BaseEntity;

/**
 * RAG知识库数据集实体
 * @author shilong.zang
 * @date 17:27 <br/>
 */
@TableName("ai_rag_qa_dataset")
public class RagQaDatasetEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -5803685552931418952L;

    /**
     * 数据集ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 数据集名称
     */
    private String name;

    /**
     * 数据集图标
     */
    private String icon;

    /**
     * 数据集说明
     */
    private String description;

    /**
     * 用户ID
     */
    private String userId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
