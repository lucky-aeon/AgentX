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
    published_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (agent_id) REFERENCES agents(id)
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
    FOREIGN KEY (agent_id) REFERENCES agents(id),
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

-- 将status字段改为enabled（布尔类型）
ALTER TABLE agents RENAME COLUMN status TO enabled;
ALTER TABLE agents ALTER COLUMN enabled TYPE boolean USING CASE WHEN enabled > 0 THEN true ELSE false END;

-- 添加发布状态相关字段
ALTER TABLE agent_versions ADD COLUMN publish_status INTEGER DEFAULT 1;
ALTER TABLE agent_versions ADD COLUMN reject_reason TEXT;
ALTER TABLE agent_versions ADD COLUMN review_time TIMESTAMP;

-- 添加注释
COMMENT ON COLUMN agent_versions.publish_status IS '发布状态：1-审核中, 2-已发布, 3-拒绝, 4-已下架';
COMMENT ON COLUMN agent_versions.reject_reason IS '审核拒绝原因';
COMMENT ON COLUMN agent_versions.review_time IS '审核时间';