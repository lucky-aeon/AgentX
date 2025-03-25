package org.xhy.domain.conversation.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 上下文实体类，管理会话的上下文窗口
 */
@TableName("context")
public class ContextEntity extends Model<ContextEntity> {

    /**
     * 上下文唯一ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 所属会话ID
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 活跃消息ID列表，JSON数组
     */
    @TableField(value = "active_messages", typeHandler = org.xhy.infrastructure.typehandler.JsonTypeHandler.class)
    private String activeMessages;

    /**
     * 历史消息摘要
     */
    @TableField("summary")
    private String summary;

    /**
     * 最后更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 无参构造函数
     */
    public ContextEntity() {
    }

    /**
     * 全参构造函数
     */
    public ContextEntity(String id, String sessionId, String activeMessages,
                         String summary, LocalDateTime updatedAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.activeMessages = activeMessages;
        this.summary = summary;
        this.updatedAt = updatedAt;
    }

    // Getter和Setter方法
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

    public String getActiveMessages() {
        return activeMessages;
    }

    public void setActiveMessages(String activeMessages) {
        this.activeMessages = activeMessages;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 创建新的上下文
     */
    public static ContextEntity createNew(String sessionId) {
        ContextEntity context = new ContextEntity();
        context.setSessionId(sessionId);
        context.setActiveMessages("[]"); // 初始化为空数组
        context.setUpdatedAt(LocalDateTime.now());
        return context;
    }

    /**
     * 添加消息到活跃消息列表
     */
    public void addMessage(String messageId) {
        // 简单实现，实际项目中应该使用JSON库处理
        // 这里假设activeMessages是一个JSON数组字符串
        List<String> messages = parseActiveMessages();
        messages.add(messageId);
        this.activeMessages = formatActiveMessages(messages);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取活跃消息ID列表
     */
    public List<String> getActiveMessageIds() {
        return parseActiveMessages();
    }

    /**
     * 设置活跃消息列表
     */
    public void setActiveMessageIds(List<String> messageIds) {
        this.activeMessages = formatActiveMessages(messageIds);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 清空上下文
     */
    public void clear() {
        this.activeMessages = "[]";
        this.summary = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 解析activeMessages字符串为List
     */
    private List<String> parseActiveMessages() {
        // 注意：实际项目中应使用JSON库解析
        if (activeMessages == null || activeMessages.equals("[]")) {
            return new ArrayList<>();
        }

        // 简单实现，实际项目中应使用JSON库解析
        String content = activeMessages.substring(1, activeMessages.length() - 1);
        if (content.isEmpty()) {
            return new ArrayList<>();
        }

        String[] ids = content.split(",");
        List<String> result = new ArrayList<>(ids.length);
        for (String id : ids) {
            result.add(id.trim().replace("\"", ""));
        }
        return result;
    }

    /**
     * 将List格式化为JSON数组字符串
     */
    private String formatActiveMessages(List<String> messages) {
        // 注意：实际项目中应使用JSON库生成
        if (messages == null || messages.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(messages.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
}