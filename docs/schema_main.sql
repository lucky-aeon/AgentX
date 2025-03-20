-- AgentX主数据库脚本
-- 创建于：2025-03-19

-- =============================================
-- 基础表结构定义
-- =============================================

-- 创建会话表
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

-- 创建消息表
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

-- 创建上下文表
CREATE TABLE context (
    id VARCHAR(36) PRIMARY KEY,           -- 上下文唯一ID
    session_id VARCHAR(36) NOT NULL,      -- 所属会话ID
    active_messages JSON,                 -- 活跃消息ID列表，当前包含在上下文中的消息
    summary TEXT,                         -- 可选，当前上下文的摘要信息
    updated_at TIMESTAMP NOT NULL,        -- 最后更新时间
    FOREIGN KEY (session_id) REFERENCES sessions(id)
);

-- =============================================
-- 索引定义
-- =============================================

-- 创建会话查询索引
CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_created_at ON sessions(created_at);
CREATE INDEX idx_sessions_updated_at ON sessions(updated_at);

-- 创建消息查询索引
CREATE INDEX idx_messages_session_id ON messages(session_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);

-- 创建上下文查询索引
CREATE INDEX idx_context_session_id ON context(session_id); 