CREATE TABLE sessions (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    agent_id VARCHAR(36) COMMENT '关联的Agent ID，指定该会话使用的Agent',
    is_archived BOOLEAN DEFAULT FALSE,
    description TEXT,
    metadata JSON
);

CREATE TABLE messages (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    token_count INTEGER,
    provider VARCHAR(50),
    model VARCHAR(50),
    metadata JSON,
    FOREIGN KEY (session_id) REFERENCES sessions(id)
);

CREATE TABLE context (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    active_messages JSON,
    summary TEXT,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (session_id) REFERENCES sessions(id)
);

CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_created_at ON sessions(created_at);
CREATE INDEX idx_sessions_updated_at ON sessions(updated_at);

CREATE INDEX idx_messages_session_id ON messages(session_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);

CREATE INDEX idx_context_session_id ON context(session_id);

-- Agent 相关表结构
CREATE TABLE agents (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    avatar VARCHAR(255),
    description TEXT,
    -- 当前编辑中的配置
    system_prompt TEXT,
    welcome_message TEXT,
    model_config JSON,
    tools JSON,
    knowledge_base_ids JSON,
    -- 版本管理
    published_version VARCHAR(36) COMMENT '当前发布的版本ID',
    status SMALLINT DEFAULT 0 COMMENT '0-私有，1-待审核，2-已上架，3-被拒绝，4-已下架',
    user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Agent版本表
CREATE TABLE agent_versions (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(36) NOT NULL,
    version_number VARCHAR(20) NOT NULL COMMENT '版本号，如1.0.0',
    system_prompt TEXT,
    welcome_message TEXT,
    model_config JSON,
    tools JSON,
    knowledge_base_ids JSON,
    change_log TEXT COMMENT '版本更新日志',
    published_at TIMESTAMP NOT NULL COMMENT '发布时间',
    FOREIGN KEY (agent_id) REFERENCES agents(id)
);

-- Agent工作区表
CREATE TABLE agent_workspace (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (agent_id) REFERENCES agents(id),
    UNIQUE (agent_id, user_id)
);

-- 创建索引
CREATE INDEX idx_agents_user_id ON agents(user_id);
CREATE INDEX idx_agents_status ON agents(status);
CREATE INDEX idx_agents_name ON agents(name);
CREATE INDEX idx_agent_versions_agent_id ON agent_versions(agent_id);
CREATE INDEX idx_agent_versions_published_at ON agent_versions(published_at);
CREATE INDEX idx_agent_workspace_user_id ON agent_workspace(user_id);
CREATE INDEX idx_agent_workspace_agent_id ON agent_workspace(agent_id);

CREATE INDEX idx_sessions_agent_id ON sessions(agent_id); 