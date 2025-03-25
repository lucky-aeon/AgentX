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
    agent_id VARCHAR(36),         -- 关联的agentId
    is_archived BOOLEAN DEFAULT FALSE,    -- 是否归档
    description TEXT,                     -- 会话描述
    metadata JSON                         -- 会话元数据，可存储其他自定义信息
);

-- 为sessions表添加注释
COMMENT ON TABLE sessions IS '用户会话表';
COMMENT ON COLUMN sessions.id IS '会话唯一ID';
COMMENT ON COLUMN sessions.title IS '会话标题';
COMMENT ON COLUMN sessions.created_at IS '创建时间';
COMMENT ON COLUMN sessions.updated_at IS '最后更新时间';
COMMENT ON COLUMN sessions.user_id IS '所属用户ID';
COMMENT ON COLUMN sessions.agent_id IS '关联的agentId';
COMMENT ON COLUMN sessions.is_archived IS '是否归档';
COMMENT ON COLUMN sessions.description IS '会话描述';
COMMENT ON COLUMN sessions.metadata IS '会话元数据，可存储其他自定义信息';

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
    metadata JSON                        -- 消息元数据
);

-- 为messages表添加注释
COMMENT ON TABLE messages IS '会话消息表';
COMMENT ON COLUMN messages.id IS '消息唯一ID';
COMMENT ON COLUMN messages.session_id IS '所属会话ID';
COMMENT ON COLUMN messages.role IS '消息角色(user/assistant/system)';
COMMENT ON COLUMN messages.content IS '消息内容';
COMMENT ON COLUMN messages.created_at IS '创建时间';
COMMENT ON COLUMN messages.token_count IS 'Token数量(可选，用于统计)';
COMMENT ON COLUMN messages.provider IS '服务提供商';
COMMENT ON COLUMN messages.model IS '使用的模型';
COMMENT ON COLUMN messages.metadata IS '消息元数据';

-- 创建上下文表
CREATE TABLE context (
    id VARCHAR(36) PRIMARY KEY,           -- 上下文唯一ID
    session_id VARCHAR(36) NOT NULL,      -- 所属会话ID
    active_messages JSON,                 -- 活跃消息ID列表，当前包含在上下文中的消息
    summary TEXT,                         -- 可选，当前上下文的摘要信息
    updated_at TIMESTAMP NOT NULL        -- 最后更新时间
);

-- 为context表添加注释
COMMENT ON TABLE context IS '会话上下文表';
COMMENT ON COLUMN context.id IS '上下文唯一ID';
COMMENT ON COLUMN context.session_id IS '所属会话ID';
COMMENT ON COLUMN context.active_messages IS '活跃消息ID列表，当前包含在上下文中的消息';
COMMENT ON COLUMN context.summary IS '当前上下文的摘要信息';
COMMENT ON COLUMN context.updated_at IS '最后更新时间';

-- =============================================
-- 索引定义
-- =============================================

-- 创建会话查询索引
CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_created_at ON sessions(created_at);
CREATE INDEX idx_sessions_updated_at ON sessions(updated_at);
CREATE INDEX idx_sessions_agent_version_id ON sessions(agent_version_id);

-- 创建消息查询索引
CREATE INDEX idx_messages_session_id ON messages(session_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);

-- 创建上下文查询索引
CREATE INDEX idx_context_session_id ON context(session_id); 

-- Agent 数据库表设计
-- 用于存储Agent的基本信息和配置

-- Agent表：核心表，存储Agent实体信息和当前编辑中的配置
CREATE TABLE agents (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    avatar VARCHAR(255),
    description TEXT,
    -- 当前编辑中的配置（未发布）
    system_prompt TEXT,
    welcome_message TEXT,
    model_config JSON,
    tools JSON,
    knowledge_base_ids JSON,
    -- 版本管理
    published_version VARCHAR(36),
    -- 状态管理
    status SMALLINT DEFAULT 0,
    agent_type SMALLINT DEFAULT 1,
    user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);

-- 为agents表字段添加注释
COMMENT ON TABLE agents IS 'Agent核心表，存储Agent实体信息和当前编辑中的配置';
COMMENT ON COLUMN agents.name IS 'Agent名称';
COMMENT ON COLUMN agents.avatar IS 'Agent头像URL';
COMMENT ON COLUMN agents.description IS 'Agent描述';
COMMENT ON COLUMN agents.system_prompt IS 'Agent系统提示词';
COMMENT ON COLUMN agents.welcome_message IS '欢迎消息';
COMMENT ON COLUMN agents.model_config IS '模型配置，包含模型类型、温度等参数';
COMMENT ON COLUMN agents.tools IS 'Agent可使用的工具列表';
COMMENT ON COLUMN agents.knowledge_base_ids IS '关联的知识库ID列表';
COMMENT ON COLUMN agents.published_version IS '当前发布的版本ID';
COMMENT ON COLUMN agents.status IS 'Agent状态：0-私有(仅创建者可见)，1-待审核(已提交待审核)，2-已上架(审核通过并公开)，3-被拒绝(审核未通过)，4-已下架(从市场移除)';
COMMENT ON COLUMN agents.agent_type IS 'Agent类型：1-聊天助手, 2-功能性Agent';
COMMENT ON COLUMN agents.user_id IS '创建者用户ID';

-- Agent版本表：存储Agent的已发布版本
CREATE TABLE agent_versions (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(36) NOT NULL,
    version_number VARCHAR(20) NOT NULL,
    system_prompt TEXT,
    welcome_message TEXT,
    model_config JSON,
    tools JSON,
    knowledge_base_ids JSON,
    agent_type SMALLINT DEFAULT 1,
    change_log TEXT,
    published_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 为agent_versions表添加注释
COMMENT ON TABLE agent_versions IS 'Agent版本表：存储Agent的已发布版本';
COMMENT ON COLUMN agent_versions.agent_id IS '关联的Agent ID';
COMMENT ON COLUMN agent_versions.version_number IS '版本号，如1.0.0';
COMMENT ON COLUMN agent_versions.system_prompt IS 'Agent系统提示词';
COMMENT ON COLUMN agent_versions.welcome_message IS '欢迎消息';
COMMENT ON COLUMN agent_versions.model_config IS '模型配置，包含模型类型、温度等参数';
COMMENT ON COLUMN agent_versions.tools IS 'Agent可使用的工具列表';
COMMENT ON COLUMN agent_versions.knowledge_base_ids IS '关联的知识库ID列表';
COMMENT ON COLUMN agent_versions.agent_type IS 'Agent类型：1-聊天助手, 2-功能性Agent';
COMMENT ON COLUMN agent_versions.change_log IS '版本更新日志';
COMMENT ON COLUMN agent_versions.published_at IS '发布时间';

-- Agent工作区表：用于记录用户添加到工作区的Agent
CREATE TABLE agent_workspace (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (agent_id, user_id)
);

-- 为agent_workspace表添加注释
COMMENT ON TABLE agent_workspace IS 'Agent工作区表：用于记录用户添加到工作区的Agent';
COMMENT ON COLUMN agent_workspace.agent_id IS '添加到工作区的Agent ID';
COMMENT ON COLUMN agent_workspace.user_id IS '用户ID';
COMMENT ON COLUMN agent_workspace.agent_id IS '添加到工作区的Agent ID';
COMMENT ON COLUMN agent_workspace.user_id IS '用户ID';
COMMENT ON CONSTRAINT agent_workspace_agent_id_user_id_key ON agent_workspace IS '防止重复添加';

-- 创建索引
CREATE INDEX idx_agents_user_id ON agents(user_id);
CREATE INDEX idx_agents_status ON agents(status);
CREATE INDEX idx_agents_name ON agents(name);
CREATE INDEX idx_agent_versions_agent_id ON agent_versions(agent_id);
CREATE INDEX idx_agent_versions_version_number ON agent_versions(version_number);
CREATE INDEX idx_agent_versions_published_at ON agent_versions(published_at);
CREATE INDEX idx_agent_workspace_user_id ON agent_workspace(user_id);
CREATE INDEX idx_agent_workspace_agent_id ON agent_workspace(agent_id); 