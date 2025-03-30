-- 创建服务商表
CREATE TABLE IF NOT EXISTS providers (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36),
    description TEXT,
    protocol VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    config TEXT NOT NULL,
    is_official BOOLEAN NOT NULL DEFAULT FALSE,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
);

COMMENT ON TABLE providers IS '服务商配置表';
COMMENT ON COLUMN providers.id IS '服务商ID';
COMMENT ON COLUMN providers.user_id IS '用户ID，官方服务商为NULL';
COMMENT ON COLUMN providers.protocol IS '服务商协议，例如：OPENAI、ANTHROPIC';
COMMENT ON COLUMN providers.description IS '服务商描述';
COMMENT ON COLUMN providers.name IS '服务商名称';
COMMENT ON COLUMN providers.config IS '服务商配置参数(加密存储)，如：{"api_key": "xxx", "api_secret": "xxx"}';
COMMENT ON COLUMN providers.is_official IS '是否官方服务商';
COMMENT ON COLUMN providers.status IS '状态：TRUE-启用，FALSE-禁用';
COMMENT ON COLUMN providers.created_at IS '创建时间';
COMMENT ON COLUMN providers.updated_at IS '更新时间';
COMMENT ON COLUMN providers.deleted_at IS '删除时间（软删除）';



-- 创建模型表
CREATE TABLE IF NOT EXISTS models (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36),
    provider_id VARCHAR(36) NOT NULL,
    model_id VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL,
    config JSONB,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (provider_id) REFERENCES providers(id)
);

COMMENT ON TABLE models IS '模型配置表';
COMMENT ON COLUMN models.id IS '模型ID';
COMMENT ON COLUMN models.user_id IS '用户ID，官方模型为NULL';
COMMENT ON COLUMN models.provider_id IS '服务商ID';
COMMENT ON COLUMN models.model_id IS '模型id，例如：gpt-4、claude-3';
COMMENT ON COLUMN models.name IS '模型名称';
COMMENT ON COLUMN models.description IS '模型描述';
COMMENT ON COLUMN models.type IS '模型类型：CHAT-对话模型，EMBEDDING-嵌入模型';
COMMENT ON COLUMN models.config IS '模型配置参数，包含：max_context_length-最大上下文长度, temperature-温度等';
COMMENT ON COLUMN models.status IS '状态：TRUE-启用，FALSE-禁用';
COMMENT ON COLUMN models.created_at IS '创建时间';
COMMENT ON COLUMN models.updated_at IS '更新时间';
COMMENT ON COLUMN models.deleted_at IS '删除时间（软删除）';

-- 创建模型索引
CREATE INDEX idx_models_user_id ON models(user_id);
CREATE INDEX idx_models_provider_id ON models(provider_id);
CREATE INDEX idx_models_type ON models(type);

-- 修改Agent工作区表，添加model_id字段
ALTER TABLE agent_workspace
    ADD COLUMN model_id VARCHAR(36),
    ADD CONSTRAINT fk_workspace_model FOREIGN KEY (model_id) REFERENCES models(id);

COMMENT ON COLUMN agent_workspace.model_id IS '用户选择的模型ID，NULL表示使用Agent默认模型';

-- 创建model_id索引
CREATE INDEX idx_workspace_model ON agent_workspace(model_id);
