-- AgentX初始化数据库脚本 - PostgreSQL版本

-- 会话表，存储用户与Agent的对话会话
CREATE TABLE sessions (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    agent_id VARCHAR(36),
    description TEXT,
    is_archived BOOLEAN DEFAULT FALSE,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 消息表，存储会话中的所有消息
CREATE TABLE messages (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    token_count INTEGER DEFAULT 0,
    provider VARCHAR(50),
    model VARCHAR(50),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 上下文表，管理对话上下文
CREATE TABLE context (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    active_messages JSONB,
    summary TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Agent 相关表结构
CREATE TABLE agents (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    avatar VARCHAR(255),
    description TEXT,
    system_prompt TEXT,
    welcome_message TEXT,
    tool_ids JSONB,
    published_version VARCHAR(36),
    enabled BOOLEAN DEFAULT TRUE,
    agent_type INTEGER DEFAULT 1,
    user_id VARCHAR(36) NOT NULL,
    tool_preset_params JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Agent版本表
CREATE TABLE agent_versions (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    avatar VARCHAR(255),
    description TEXT,
    version_number VARCHAR(20) NOT NULL,
    system_prompt TEXT,
    welcome_message TEXT,
    tool_ids JSONB,
    knowledge_base_ids JSONB,
    change_log TEXT,
    agent_type INTEGER DEFAULT 1,
    publish_status INTEGER DEFAULT 1,
    reject_reason TEXT,
    review_time TIMESTAMP,
    published_at TIMESTAMP,
    user_id VARCHAR(36) NOT NULL,
    tool_preset_params JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Agent工作区表
CREATE TABLE agent_workspace (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    llm_model_config JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 服务提供商表
CREATE TABLE providers (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36),
    protocol VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    config TEXT,
    is_official BOOLEAN DEFAULT FALSE,
    status BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 模型表
CREATE TABLE models (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36),
    provider_id VARCHAR(36) NOT NULL,
    model_id VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_official BOOLEAN DEFAULT FALSE,
    type VARCHAR(20) NOT NULL,
    status BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 任务表
CREATE TABLE agent_tasks (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    parent_task_id VARCHAR(36),
    task_name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20),
    progress INTEGER DEFAULT 0,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    task_result TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 定时任务表
CREATE TABLE scheduled_tasks (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    agent_id VARCHAR(36) NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    repeat_type VARCHAR(20) NOT NULL,
    repeat_config JSONB,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    last_execute_time TIMESTAMP,
    next_execute_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE users (
                             id varchar(36) PRIMARY KEY,
                             nickname varchar(255) NOT NULL,
                             email varchar(255),
                             phone varchar(11),
                             password varchar NOT NULL,
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             deleted_at TIMESTAMP,
                             github_id varchar(255),
                             github_login varchar(255),
                             avatar_url varchar(255)
);

-- 工具相关表

-- 工具表
CREATE TABLE tools (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    icon VARCHAR(255),
    subtitle VARCHAR(255),
    description TEXT,
    user_id VARCHAR(36) NOT NULL,
    labels JSONB,
    tool_type VARCHAR(50) NOT NULL,
    upload_type VARCHAR(20) NOT NULL,
    upload_url VARCHAR(255),
    install_command JSONB,
    tool_list JSONB,
    reject_reason TEXT,
    failed_step_status VARCHAR(20),
    mcp_server_name VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    is_office BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 工具版本表
CREATE TABLE tool_versions (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    icon VARCHAR(255),
    subtitle VARCHAR(255),
    description TEXT,
    user_id VARCHAR(36) NOT NULL,
    version VARCHAR(50) NOT NULL,
    tool_id VARCHAR(36) NOT NULL,
    upload_type VARCHAR(20) NOT NULL,
    change_log TEXT,
    upload_url VARCHAR(255),
    tool_list JSONB,
    labels JSONB,
    mcp_server_name VARCHAR(255),
    is_office BOOLEAN DEFAULT FALSE,
    public_status BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 用户工具关联表
CREATE TABLE user_tools (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    icon VARCHAR(255),
    subtitle VARCHAR(255),
    tool_id VARCHAR(36) NOT NULL,
    version VARCHAR(50) NOT NULL,
    tool_list JSONB,
    labels JSONB,
    is_office BOOLEAN DEFAULT FALSE,
    public_state BOOLEAN DEFAULT FALSE,
    mcp_server_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);


-- 用户设置表
CREATE TABLE user_settings (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE,
    setting_config JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 添加索引
CREATE INDEX idx_user_settings_user_id ON user_settings(user_id);





-- 创建索引
CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_agent_id ON sessions(agent_id);
CREATE INDEX idx_context_session_id ON context(session_id);
CREATE INDEX idx_messages_session_id ON messages(session_id);
CREATE INDEX idx_agent_tasks_session_id ON agent_tasks(session_id);
CREATE INDEX idx_agent_tasks_user_id ON agent_tasks(user_id);
CREATE INDEX idx_agent_tasks_parent_task_id ON agent_tasks(parent_task_id);
CREATE INDEX idx_agents_user_id ON agents(user_id);
CREATE INDEX idx_agent_workspace_agent_id ON agent_workspace(agent_id);
CREATE INDEX idx_agent_workspace_user_id ON agent_workspace(user_id);
CREATE INDEX idx_agent_versions_agent_id ON agent_versions(agent_id);
CREATE INDEX idx_agent_versions_user_id ON agent_versions(user_id);
CREATE INDEX idx_models_provider_id ON models(provider_id);
CREATE INDEX idx_models_user_id ON models(user_id);
CREATE INDEX idx_providers_user_id ON providers(user_id);
CREATE INDEX idx_tools_user_id ON tools(user_id);
CREATE INDEX idx_scheduled_tasks_user_id ON scheduled_tasks(user_id);
CREATE INDEX idx_scheduled_tasks_agent_id ON scheduled_tasks(agent_id);
CREATE INDEX idx_scheduled_tasks_session_id ON scheduled_tasks(session_id);
CREATE INDEX idx_scheduled_tasks_status ON scheduled_tasks(status);
CREATE INDEX idx_user_settings_user_id ON user_settings(user_id);

-- 添加表和列的注释
COMMENT ON TABLE sessions IS '会话实体类，代表一个独立的对话会话/主题';
COMMENT ON COLUMN sessions.id IS '会话唯一ID';
COMMENT ON COLUMN sessions.title IS '会话标题';
COMMENT ON COLUMN sessions.user_id IS '所属用户ID';
COMMENT ON COLUMN sessions.agent_id IS '关联的Agent版本ID';
COMMENT ON COLUMN sessions.description IS '会话描述';
COMMENT ON COLUMN sessions.is_archived IS '是否归档';
COMMENT ON COLUMN sessions.metadata IS '会话元数据，可存储其他自定义信息，JSON格式';
COMMENT ON COLUMN sessions.created_at IS '创建时间';
COMMENT ON COLUMN sessions.updated_at IS '更新时间';
COMMENT ON COLUMN sessions.deleted_at IS '逻辑删除时间';

COMMENT ON TABLE messages IS '消息实体类，代表对话中的一条消息';
COMMENT ON COLUMN messages.id IS '消息唯一ID';
COMMENT ON COLUMN messages.session_id IS '所属会话ID';
COMMENT ON COLUMN messages.role IS '消息角色 (user, assistant, system)';
COMMENT ON COLUMN messages.content IS '消息内容';
COMMENT ON COLUMN messages.message_type IS '消息类型';
COMMENT ON COLUMN messages.token_count IS 'Token数量';
COMMENT ON COLUMN messages.provider IS '服务提供商';
COMMENT ON COLUMN messages.model IS '使用的模型';
COMMENT ON COLUMN messages.metadata IS '消息元数据，JSON格式';
COMMENT ON COLUMN messages.created_at IS '创建时间';
COMMENT ON COLUMN messages.updated_at IS '更新时间';
COMMENT ON COLUMN messages.deleted_at IS '逻辑删除时间';

COMMENT ON TABLE context IS '上下文实体类，管理会话的上下文窗口';
COMMENT ON COLUMN context.id IS '上下文唯一ID';
COMMENT ON COLUMN context.session_id IS '所属会话ID';
COMMENT ON COLUMN context.active_messages IS '活跃消息ID列表，JSON数组格式';
COMMENT ON COLUMN context.summary IS '历史消息摘要';
COMMENT ON COLUMN context.created_at IS '创建时间';
COMMENT ON COLUMN context.updated_at IS '更新时间';
COMMENT ON COLUMN context.deleted_at IS '逻辑删除时间';

COMMENT ON TABLE agents IS 'Agent实体类，代表一个AI助手';
COMMENT ON COLUMN agents.id IS 'Agent唯一ID';
COMMENT ON COLUMN agents.name IS 'Agent名称';
COMMENT ON COLUMN agents.avatar IS 'Agent头像URL';
COMMENT ON COLUMN agents.description IS 'Agent描述';
COMMENT ON COLUMN agents.system_prompt IS 'Agent系统提示词';
COMMENT ON COLUMN agents.welcome_message IS '欢迎消息';
COMMENT ON COLUMN agents.published_version IS '当前发布的版本ID';
COMMENT ON COLUMN agents.enabled IS 'Agent状态：TRUE-启用，FALSE-禁用';
COMMENT ON COLUMN agents.agent_type IS 'Agent类型：1-聊天助手, 2-功能性Agent';
COMMENT ON COLUMN agents.user_id IS '创建者用户ID';
COMMENT ON COLUMN agents.created_at IS '创建时间';
COMMENT ON COLUMN agents.updated_at IS '更新时间';
COMMENT ON COLUMN agents.deleted_at IS '逻辑删除时间';

COMMENT ON TABLE agent_versions IS 'Agent版本实体类，代表一个Agent的发布版本';
COMMENT ON COLUMN agent_versions.id IS '版本唯一ID';
COMMENT ON COLUMN agent_versions.agent_id IS '关联的Agent ID';
COMMENT ON COLUMN agent_versions.name IS 'Agent名称';
COMMENT ON COLUMN agent_versions.avatar IS 'Agent头像URL';
COMMENT ON COLUMN agent_versions.description IS 'Agent描述';
COMMENT ON COLUMN agent_versions.version_number IS '版本号，如1.0.0';
COMMENT ON COLUMN agent_versions.system_prompt IS 'Agent系统提示词';
COMMENT ON COLUMN agent_versions.welcome_message IS '欢迎消息';
COMMENT ON COLUMN agent_versions.tools IS 'Agent可使用的工具列表，JSON数组格式';
COMMENT ON COLUMN agent_versions.knowledge_base_ids IS '关联的知识库ID列表，JSON数组格式';
COMMENT ON COLUMN agent_versions.change_log IS '版本更新日志';
COMMENT ON COLUMN agent_versions.agent_type IS 'Agent类型：1-聊天助手, 2-功能性Agent';
COMMENT ON COLUMN agent_versions.publish_status IS '发布状态：1-审核中, 2-已发布, 3-拒绝, 4-已下架';
COMMENT ON COLUMN agent_versions.reject_reason IS '审核拒绝原因';
COMMENT ON COLUMN agent_versions.review_time IS '审核时间';
COMMENT ON COLUMN agent_versions.published_at IS '发布时间';
COMMENT ON COLUMN agent_versions.user_id IS '创建者用户ID';
COMMENT ON COLUMN agent_versions.created_at IS '创建时间';
COMMENT ON COLUMN agent_versions.updated_at IS '更新时间';
COMMENT ON COLUMN agent_versions.deleted_at IS '逻辑删除时间';

COMMENT ON TABLE agent_workspace IS 'Agent工作区实体类，用于记录用户添加到工作区的Agent';
COMMENT ON COLUMN agent_workspace.id IS '主键ID';
COMMENT ON COLUMN agent_workspace.agent_id IS 'Agent ID';
COMMENT ON COLUMN agent_workspace.user_id IS '用户ID';
COMMENT ON COLUMN agent_workspace.llm_model_config IS '模型配置，JSON格式';
COMMENT ON COLUMN agent_workspace.created_at IS '创建时间';
COMMENT ON COLUMN agent_workspace.updated_at IS '更新时间';
COMMENT ON COLUMN agent_workspace.deleted_at IS '逻辑删除时间';

COMMENT ON TABLE providers IS '服务提供商领域模型';
COMMENT ON COLUMN providers.id IS '服务提供商ID';
COMMENT ON COLUMN providers.user_id IS '用户ID';
COMMENT ON COLUMN providers.protocol IS '协议类型';
COMMENT ON COLUMN providers.name IS '服务提供商名称';
COMMENT ON COLUMN providers.description IS '服务提供商描述';
COMMENT ON COLUMN providers.config IS '服务提供商配置,加密后的值';
COMMENT ON COLUMN providers.is_official IS '是否官方服务提供商';
COMMENT ON COLUMN providers.status IS '服务提供商状态';
COMMENT ON COLUMN providers.created_at IS '创建时间';
COMMENT ON COLUMN providers.updated_at IS '更新时间';
COMMENT ON COLUMN providers.deleted_at IS '逻辑删除时间';

COMMENT ON TABLE models IS '模型领域模型';
COMMENT ON COLUMN models.id IS '模型ID';
COMMENT ON COLUMN models.user_id IS '用户ID';
COMMENT ON COLUMN models.provider_id IS '服务提供商ID';
COMMENT ON COLUMN models.model_id IS '模型ID标识';
COMMENT ON COLUMN models.name IS '模型名称';
COMMENT ON COLUMN models.description IS '模型描述';
COMMENT ON COLUMN models.is_official IS '是否官方模型';
COMMENT ON COLUMN models.type IS '模型类型';
COMMENT ON COLUMN models.status IS '模型状态';
COMMENT ON COLUMN models.created_at IS '创建时间';
COMMENT ON COLUMN models.updated_at IS '更新时间';
COMMENT ON COLUMN models.deleted_at IS '逻辑删除时间';

COMMENT ON TABLE agent_tasks IS '任务实体类';
COMMENT ON COLUMN agent_tasks.id IS '任务ID';
COMMENT ON COLUMN agent_tasks.session_id IS '所属会话ID';
COMMENT ON COLUMN agent_tasks.user_id IS '用户ID';
COMMENT ON COLUMN agent_tasks.parent_task_id IS '父任务ID';
COMMENT ON COLUMN agent_tasks.task_name IS '任务名称';
COMMENT ON COLUMN agent_tasks.description IS '任务描述';
COMMENT ON COLUMN agent_tasks.status IS '任务状态';
COMMENT ON COLUMN agent_tasks.progress IS '任务进度,存放父任务中';
COMMENT ON COLUMN agent_tasks.start_time IS '开始时间';
COMMENT ON COLUMN agent_tasks.end_time IS '结束时间';
COMMENT ON COLUMN agent_tasks.task_result IS '任务结果';
COMMENT ON COLUMN agent_tasks.created_at IS '创建时间';
COMMENT ON COLUMN agent_tasks.updated_at IS '更新时间';
COMMENT ON COLUMN agent_tasks.deleted_at IS '逻辑删除时间';

COMMENT ON TABLE scheduled_tasks IS '定时任务实体类';
COMMENT ON COLUMN scheduled_tasks.id IS '定时任务唯一ID';
COMMENT ON COLUMN scheduled_tasks.user_id IS '用户ID';
COMMENT ON COLUMN scheduled_tasks.agent_id IS '关联的Agent ID';
COMMENT ON COLUMN scheduled_tasks.session_id IS '关联的会话ID';
COMMENT ON COLUMN scheduled_tasks.content IS '任务内容';
COMMENT ON COLUMN scheduled_tasks.repeat_type IS '重复类型：NONE-不重复, DAILY-每天, WEEKLY-每周, MONTHLY-每月, WORKDAYS-工作日, CUSTOM-自定义';
COMMENT ON COLUMN scheduled_tasks.repeat_config IS '重复配置，JSON格式存储具体的重复规则';
COMMENT ON COLUMN scheduled_tasks.status IS '任务状态：ACTIVE-活跃, PAUSED-暂停, COMPLETED-已完成';
COMMENT ON COLUMN scheduled_tasks.last_execute_time IS '上次执行时间';
COMMENT ON COLUMN scheduled_tasks.created_at IS '创建时间';
COMMENT ON COLUMN scheduled_tasks.updated_at IS '更新时间';
COMMENT ON COLUMN scheduled_tasks.deleted_at IS '逻辑删除时间';

COMMENT ON COLUMN users.id IS '主键';
COMMENT ON COLUMN users.nickname IS '昵称';
COMMENT ON COLUMN users.email IS '邮箱';
COMMENT ON COLUMN users.phone IS '手机号';
COMMENT ON COLUMN users.password IS '密码';
COMMENT ON COLUMN users.created_at IS '创建时间';
COMMENT ON COLUMN users.updated_at IS '更新时间';
COMMENT ON COLUMN users.deleted_at IS '逻辑删除时间';

COMMENT ON TABLE tools IS '工具实体类';
COMMENT ON COLUMN tools.id IS '工具唯一ID';
COMMENT ON COLUMN tools.name IS '工具名称';
COMMENT ON COLUMN tools.icon IS '工具图标';
COMMENT ON COLUMN tools.subtitle IS '副标题';
COMMENT ON COLUMN tools.description IS '工具描述';
COMMENT ON COLUMN tools.user_id IS '用户ID';
COMMENT ON COLUMN tools.labels IS '标签列表，JSON数组格式';
COMMENT ON COLUMN tools.tool_type IS '工具类型';
COMMENT ON COLUMN tools.upload_type IS '上传方式';
COMMENT ON COLUMN tools.upload_url IS '上传URL';
COMMENT ON COLUMN tools.install_command IS '安装命令，JSON格式';
COMMENT ON COLUMN tools.tool_list IS '工具列表，JSON数组格式';
COMMENT ON COLUMN tools.status IS '审核状态';
COMMENT ON COLUMN tools.is_office IS '是否官方工具';
COMMENT ON COLUMN tools.created_at IS '创建时间';
COMMENT ON COLUMN tools.updated_at IS '更新时间';
COMMENT ON COLUMN tools.deleted_at IS '逻辑删除时间';

COMMENT ON TABLE tool_versions IS '工具版本实体类';
COMMENT ON COLUMN tool_versions.id IS '版本唯一ID';
COMMENT ON COLUMN tool_versions.name IS '工具名称';
COMMENT ON COLUMN tool_versions.icon IS '工具图标';
COMMENT ON COLUMN tool_versions.subtitle IS '副标题';
COMMENT ON COLUMN tool_versions.description IS '工具描述';
COMMENT ON COLUMN tool_versions.user_id IS '用户ID';
COMMENT ON COLUMN tool_versions.version IS '版本号';
COMMENT ON COLUMN tool_versions.tool_id IS '工具ID';
COMMENT ON COLUMN tool_versions.upload_type IS '上传方式';
COMMENT ON COLUMN tool_versions.upload_url IS '上传URL';
COMMENT ON COLUMN tool_versions.tool_list IS '工具列表，JSON数组格式';
COMMENT ON COLUMN tool_versions.labels IS '标签列表，JSON数组格式';
COMMENT ON COLUMN tool_versions.is_office IS '是否官方工具';
COMMENT ON COLUMN tool_versions.public_status IS '公开状态';
COMMENT ON COLUMN tool_versions.created_at IS '创建时间';
COMMENT ON COLUMN tool_versions.updated_at IS '更新时间';
COMMENT ON COLUMN tool_versions.deleted_at IS '逻辑删除时间';

COMMENT ON TABLE user_tools IS '用户工具关联实体类';
COMMENT ON COLUMN user_tools.id IS '唯一ID';
COMMENT ON COLUMN user_tools.user_id IS '用户ID';
COMMENT ON COLUMN user_tools.tool_version_id IS '工具版本ID';
COMMENT ON COLUMN user_tools.version IS '版本号';
COMMENT ON COLUMN user_tools.upload_type IS '上传方式';
COMMENT ON COLUMN user_tools.upload_url IS '上传URL';
COMMENT ON COLUMN user_tools.tool_list IS '工具列表，JSON数组格式';
COMMENT ON COLUMN user_tools.labels IS '标签列表，JSON数组格式';
COMMENT ON COLUMN user_tools.is_office IS '是否官方工具';
COMMENT ON COLUMN user_tools.public_state IS '公开状态';
COMMENT ON COLUMN user_tools.created_at IS '创建时间';
COMMENT ON COLUMN user_tools.updated_at IS '更新时间';
COMMENT ON COLUMN user_tools.deleted_at IS '逻辑删除时间';
    
-- 添加表和列的注释
COMMENT ON TABLE user_settings IS '用户设置表，存储用户的个性化配置';
COMMENT ON COLUMN user_settings.id IS '设置记录唯一ID';
COMMENT ON COLUMN user_settings.user_id IS '用户ID，关联users表';
COMMENT ON COLUMN user_settings.setting_config IS '设置配置JSON，格式：{"default_model": "模型ID"}';
COMMENT ON COLUMN user_settings.created_at IS '创建时间';
COMMENT ON COLUMN user_settings.updated_at IS '更新时间';
COMMENT ON COLUMN user_settings.deleted_at IS '逻辑删除时间';