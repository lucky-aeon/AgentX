package org.xhy.domain.conversation.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.ArrayList;
import java.util.List;
import org.xhy.infrastructure.converter.ListConverter;
import org.xhy.infrastructure.entity.BaseEntity;

/** 上下文实体类，管理会话的上下文窗口 */
@TableName("context")
public class ContextEntity extends BaseEntity {

    /** 上下文唯一ID */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 所属会话ID */
    @TableField("session_id")
    private String sessionId;

    /** 活跃消息ID列表 */
    @TableField(value = "active_messages", typeHandler = ListConverter.class)
    private List<String> activeMessages = new ArrayList<>();

    /** 历史消息摘要 */
    @TableField("summary")
    private String summary;

    /** 无参构造函数 */
    public ContextEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<String> getActiveMessages() {
        return activeMessages;
    }

    public void setActiveMessages(List<String> activeMessages) {
        this.activeMessages = activeMessages;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
