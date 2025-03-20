# 数据库表设计

## 会话表 (sessions)
```sql
CREATE TABLE sessions (
    id VARCHAR(36) PRIMARY KEY,           -- 会话唯一ID
    title VARCHAR(255) NOT NULL,          -- 会话标题
    created_at TIMESTAMP NOT NULL,        -- 创建时间
    updated_at TIMESTAMP NOT NULL,        -- 最后更新时间
    user_id VARCHAR(36) NOT NULL,         -- 所属用户ID
    is_archived BOOLEAN DEFAULT FALSE,    -- 是否归档
    description TEXT,                     -- 会话描述
    metadata JSON                         -- 会话元数据，可存储其他自定义信息
);
```

## 消息表 (messages)
```sql
CREATE TABLE messages (
    id VARCHAR(36) PRIMARY KEY,           -- 消息唯一ID
    session_id VARCHAR(36) NOT NULL,      -- 所属会话ID
    role VARCHAR(20) NOT NULL,            -- 消息角色(user/assistant/system)
    content TEXT NOT NULL,                -- 消息内容
    created_at TIMESTAMP NOT NULL,        -- 创建时间
    token_count INTEGER,                  -- Token数量(可选，用于统计)
    provider VARCHAR(50),                 -- 服务提供商
    model VARCHAR(50),                    -- 使用的模型
    metadata JSON,                        -- 消息元数据
    FOREIGN KEY (session_id) REFERENCES sessions(id)
);
```

## 上下文表 (context)
```sql
CREATE TABLE context (
    id VARCHAR(36) PRIMARY KEY,           -- 上下文唯一ID
    session_id VARCHAR(36) NOT NULL,      -- 所属会话ID
    active_messages JSON,                 -- 活跃消息ID列表，当前包含在上下文中的消息
    summary TEXT,                         -- 可选，当前上下文的摘要信息
    updated_at TIMESTAMP NOT NULL,        -- 最后更新时间
    FOREIGN KEY (session_id) REFERENCES sessions(id)
);
```

## 话题关联表 (topic_relations)
```sql
CREATE TABLE topic_relations (
    id VARCHAR(36) PRIMARY KEY,           -- 关联唯一ID
    parent_id VARCHAR(36) NOT NULL,       -- 父话题ID
    child_id VARCHAR(36) NOT NULL,        -- 子话题ID
    relation_type VARCHAR(50),            -- 关联类型
    created_at TIMESTAMP NOT NULL,        -- 创建时间
    metadata JSON,                        -- 关联元数据
    FOREIGN KEY (parent_id) REFERENCES sessions(id),
    FOREIGN KEY (child_id) REFERENCES sessions(id)
);
```

## 索引设计

```sql
-- 会话查询索引
CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_created_at ON sessions(created_at);
CREATE INDEX idx_sessions_updated_at ON sessions(updated_at);

-- 消息查询索引
CREATE INDEX idx_messages_session_id ON messages(session_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);

-- 上下文查询索引
CREATE INDEX idx_context_session_id ON context(session_id);

-- 话题关联索引
CREATE INDEX idx_topic_relations_parent ON topic_relations(parent_id);
CREATE INDEX idx_topic_relations_child ON topic_relations(child_id);
```

