-- AgentX 数据库初始化脚本 - PostgreSQL版本

-- 会话表
CREATE TABLE IF NOT EXISTS sessions (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255),
    user_id VARCHAR(36),
    agent_id VARCHAR(36),
    description TEXT,
    is_archived BOOLEAN DEFAULT FALSE,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- 消息表
CREATE TABLE IF NOT EXISTS messages (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    token_count INTEGER DEFAULT 0,
    provider VARCHAR(100),
    model VARCHAR(100),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- 上下文表
CREATE TABLE IF NOT EXISTS context (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    active_messages TEXT,
    summary TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- 模型表
CREATE TABLE IF NOT EXISTS models (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36),
    provider_id VARCHAR(36),
    model_id VARCHAR(100),
    name VARCHAR(255),
    description TEXT,
    is_official BOOLEAN DEFAULT FALSE,
    type VARCHAR(50),
    status BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- 服务提供商表
CREATE TABLE IF NOT EXISTS providers (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36),
    protocol VARCHAR(50),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    config TEXT,
    is_official BOOLEAN DEFAULT FALSE,
    status BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- Agent 相关表结构
CREATE TABLE IF NOT EXISTS agents (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    avatar VARCHAR(255),
    description TEXT,
    system_prompt TEXT,
    welcome_message TEXT,
    model_config JSONB,
    tools JSONB,
    knowledge_base_ids JSONB,
    published_version VARCHAR(36),
    enabled BOOLEAN DEFAULT TRUE,
    agent_type SMALLINT DEFAULT 1,
    user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- Agent版本表
CREATE TABLE IF NOT EXISTS agent_versions (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    avatar VARCHAR(255),
    description TEXT,
    version_number VARCHAR(20) NOT NULL,
    system_prompt TEXT,
    welcome_message TEXT,
    model_config JSONB,
    tools JSONB,
    knowledge_base_ids JSONB,
    change_log TEXT,
    agent_type SMALLINT DEFAULT 1,
    publish_status SMALLINT DEFAULT 1,
    reject_reason TEXT,
    review_time TIMESTAMP,
    published_at TIMESTAMP,
    user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- Agent工作区表
CREATE TABLE IF NOT EXISTS agent_workspace (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    llm_model_config JSONB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    UNIQUE (agent_id, user_id)
);

-- 创建索引
CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_created_at ON sessions(created_at);
CREATE INDEX idx_sessions_updated_at ON sessions(updated_at);
CREATE INDEX idx_sessions_agent_id ON sessions(agent_id);
CREATE INDEX idx_messages_session_id ON messages(session_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);
CREATE INDEX idx_context_session_id ON context(session_id);
CREATE INDEX idx_agents_user_id ON agents(user_id);
CREATE INDEX idx_agents_enabled ON agents(enabled);
CREATE INDEX idx_agents_agent_type ON agents(agent_type);
CREATE INDEX idx_agents_name ON agents(name);
CREATE INDEX idx_agent_versions_agent_id ON agent_versions(agent_id);
CREATE INDEX idx_agent_versions_published_at ON agent_versions(published_at);
CREATE INDEX idx_agent_versions_publish_status ON agent_versions(publish_status);
CREATE INDEX idx_agent_workspace_user_id ON agent_workspace(user_id);
CREATE INDEX idx_agent_workspace_agent_id ON agent_workspace(agent_id);
CREATE INDEX idx_providers_user_id ON providers(user_id);
CREATE INDEX idx_models_provider_id ON models(provider_id);
CREATE INDEX idx_models_user_id ON models(user_id);
CREATE INDEX idx_models_status ON models(status);
CREATE INDEX idx_providers_status ON providers(status);
CREATE INDEX idx_providers_name ON providers(name);

-- 添加表和列的注释
COMMENT ON TABLE sessions IS '会话表，存储用户与Agent的对话会话';
COMMENT ON COLUMN sessions.id IS '会话唯一ID';
COMMENT ON COLUMN sessions.title IS '会话标题';
COMMENT ON COLUMN sessions.user_id IS '所属用户ID';
COMMENT ON COLUMN sessions.agent_id IS '关联的Agent版本ID';
COMMENT ON COLUMN sessions.description IS '会话描述';
COMMENT ON COLUMN sessions.is_archived IS '会话是否被归档';
COMMENT ON COLUMN sessions.metadata IS '会话元数据，可存储其他自定义信息';
COMMENT ON COLUMN sessions.created_at IS '创建时间';
COMMENT ON COLUMN sessions.updated_at IS '更新时间';
COMMENT ON COLUMN sessions.deleted_at IS '删除时间（逻辑删除）';

COMMENT ON TABLE messages IS '对话消息表';
COMMENT ON COLUMN messages.id IS '消息唯一ID';
COMMENT ON COLUMN messages.session_id IS '所属会话ID';
COMMENT ON COLUMN messages.role IS '消息角色(user/assistant/system)';
COMMENT ON COLUMN messages.content IS '消息内容';
COMMENT ON COLUMN messages.token_count IS 'Token数量';
COMMENT ON COLUMN messages.provider IS '服务提供商';
COMMENT ON COLUMN messages.model IS '使用的模型';
COMMENT ON COLUMN messages.metadata IS '消息元数据';
COMMENT ON COLUMN messages.created_at IS '创建时间';
COMMENT ON COLUMN messages.updated_at IS '更新时间';
COMMENT ON COLUMN messages.deleted_at IS '删除时间（逻辑删除）';

COMMENT ON TABLE context IS '会话上下文表';
COMMENT ON COLUMN context.id IS '上下文唯一ID';
COMMENT ON COLUMN context.session_id IS '所属会话ID';
COMMENT ON COLUMN context.active_messages IS '活跃消息ID列表';
COMMENT ON COLUMN context.summary IS '历史消息摘要';
COMMENT ON COLUMN context.created_at IS '创建时间';
COMMENT ON COLUMN context.updated_at IS '更新时间';
COMMENT ON COLUMN context.deleted_at IS '删除时间（逻辑删除）';

COMMENT ON TABLE agents IS 'Agent表，存储AI助手的基本信息和配置';
COMMENT ON COLUMN agents.published_version IS '当前发布的版本ID';
COMMENT ON COLUMN agents.enabled IS 'Agent状态：false-禁用，true-启用';
COMMENT ON COLUMN agents.agent_type IS 'Agent类型：1-聊天助手, 2-功能性Agent';
COMMENT ON COLUMN agents.model_config IS '模型配置，JSON格式，包含模型类型、温度等参数';
COMMENT ON COLUMN agents.tools IS 'Agent可使用的工具列表，JSON格式';
COMMENT ON COLUMN agents.knowledge_base_ids IS '关联的知识库ID列表，JSON格式';
COMMENT ON COLUMN agents.deleted_at IS '软删除标记，非空表示已删除';

COMMENT ON TABLE agent_versions IS 'Agent版本表，记录Agent的各个版本';
COMMENT ON COLUMN agent_versions.version_number IS '版本号，如1.0.0';
COMMENT ON COLUMN agent_versions.change_log IS '版本更新日志';
COMMENT ON COLUMN agent_versions.publish_status IS '发布状态：1-审核中, 2-已发布, 3-拒绝, 4-已下架';
COMMENT ON COLUMN agent_versions.reject_reason IS '审核拒绝原因';
COMMENT ON COLUMN agent_versions.published_at IS '发布时间';
COMMENT ON COLUMN agent_versions.deleted_at IS '软删除标记';

COMMENT ON TABLE agent_workspace IS 'Agent工作区表，记录用户添加到工作区的Agent';
COMMENT ON COLUMN agent_workspace.llm_model_config IS 'LLM模型配置';

COMMENT ON TABLE providers IS 'LLM服务提供商表';
COMMENT ON COLUMN providers.id IS '提供商唯一ID';
COMMENT ON COLUMN providers.user_id IS '所属用户ID';
COMMENT ON COLUMN providers.protocol IS '协议类型';
COMMENT ON COLUMN providers.name IS '提供商名称';
COMMENT ON COLUMN providers.description IS '提供商描述';
COMMENT ON COLUMN providers.config IS '提供商配置JSON';
COMMENT ON COLUMN providers.is_official IS '是否官方提供商';
COMMENT ON COLUMN providers.status IS '状态(是否激活)';
COMMENT ON COLUMN providers.created_at IS '创建时间';
COMMENT ON COLUMN providers.updated_at IS '更新时间';
COMMENT ON COLUMN providers.deleted_at IS '删除时间（逻辑删除）';

COMMENT ON TABLE models IS 'LLM模型表';
COMMENT ON COLUMN models.id IS '模型唯一ID';
COMMENT ON COLUMN models.user_id IS '所属用户ID';
COMMENT ON COLUMN models.provider_id IS '服务提供商ID';
COMMENT ON COLUMN models.model_id IS '模型标识符';
COMMENT ON COLUMN models.name IS '模型名称';
COMMENT ON COLUMN models.description IS '模型描述';
COMMENT ON COLUMN models.is_official IS '是否官方模型';
COMMENT ON COLUMN models.type IS '模型类型';
COMMENT ON COLUMN models.status IS '模型状态(是否激活)';
COMMENT ON COLUMN models.created_at IS '创建时间';
COMMENT ON COLUMN models.updated_at IS '更新时间';
COMMENT ON COLUMN models.deleted_at IS '删除时间（逻辑删除）';
