package org.xhy.domain.agent.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.type.JdbcType;
import org.xhy.infrastructure.converter.LLMModelConfigConverter;
import org.xhy.infrastructure.entity.BaseEntity;

/** Agent工作区实体类 用于记录用户添加到工作区的Agent */
@TableName(value = "agent_workspace", autoResultMap = true)
public class AgentWorkspaceEntity extends BaseEntity {

    /** 主键ID */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** Agent ID */
    @TableField("agent_id")
    private String agentId;

    /** 用户ID */
    @TableField("user_id")
    private String userId;

    /** 模型配置 */
    @TableField(value = "llm_model_config", typeHandler = LLMModelConfigConverter.class, jdbcType = JdbcType.OTHER)
    private LLMModelConfig llmModelConfig;

    public AgentWorkspaceEntity() {
    }

    public AgentWorkspaceEntity(String agentId, String userId, LLMModelConfig llmModelConfig) {
        this.agentId = agentId;
        this.userId = userId;
        this.llmModelConfig = llmModelConfig;
    }

    public LLMModelConfig getLlmModelConfig() {
        // 兜底
        if (llmModelConfig == null) {
            llmModelConfig = new LLMModelConfig();
        }
        return llmModelConfig;
    }

    public void setLlmModelConfig(LLMModelConfig llmModelConfig) {
        this.llmModelConfig = llmModelConfig;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

}
