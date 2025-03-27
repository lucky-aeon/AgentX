-- 创建服务商表
CREATE TABLE IF NOT EXISTS providers (
    id VARCHAR(36) PRIMARY KEY COMMENT '服务商ID',
    user_id VARCHAR(36) COMMENT '用户ID，官方服务商为NULL',
    code VARCHAR(50) NOT NULL COMMENT '服务商代码，例如：OPENAI、ANTHROPIC',
    name VARCHAR(100) NOT NULL COMMENT '服务商名称',
    config TEXT NOT NULL COMMENT '服务商配置参数(加密存储)，如：{"api_key": "xxx", "api_secret": "xxx"}',
    is_official BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否官方服务商',
    status BOOLEAN NOT NULL DEFAULT TRUE COMMENT '状态：TRUE-启用，FALSE-禁用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at TIMESTAMP NULL COMMENT '删除时间（软删除）',
    UNIQUE KEY uk_providers_user_code (user_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务商配置表';

-- 创建服务商索引
CREATE INDEX idx_providers_user_id ON providers(user_id);
CREATE INDEX idx_providers_code ON providers(code);

-- 创建模型表
CREATE TABLE IF NOT EXISTS models (
    id VARCHAR(36) PRIMARY KEY COMMENT '模型ID',
    user_id VARCHAR(36) COMMENT '用户ID，官方模型为NULL',
    provider_id VARCHAR(36) NOT NULL COMMENT '服务商ID',
    code VARCHAR(100) NOT NULL COMMENT '模型代码，例如：gpt-4、claude-3',
    name VARCHAR(100) NOT NULL COMMENT '模型名称',
    description TEXT COMMENT '模型描述',
    type VARCHAR(20) NOT NULL COMMENT '模型类型：CHAT-对话模型，EMBEDDING-嵌入模型',
    config JSON COMMENT '模型配置参数，包含：max_context_length-最大上下文长度, temperature-温度等',
    is_official BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否官方模型',
    status BOOLEAN NOT NULL DEFAULT TRUE COMMENT '状态：TRUE-启用，FALSE-禁用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at TIMESTAMP NULL COMMENT '删除时间（软删除）',
    UNIQUE KEY uk_models_user_provider_code (user_id, provider_id, code),
    FOREIGN KEY (provider_id) REFERENCES providers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型配置表';

-- 创建模型索引
CREATE INDEX idx_models_user_id ON models(user_id);
CREATE INDEX idx_models_provider_id ON models(provider_id);
CREATE INDEX idx_models_type ON models(type);

-- 修改Agent工作区表，添加model_id字段
ALTER TABLE agent_workspace 
ADD COLUMN model_id VARCHAR(36) COMMENT '用户选择的模型ID，NULL表示使用Agent默认模型',
ADD CONSTRAINT fk_workspace_model FOREIGN KEY (model_id) REFERENCES models(id);

-- 创建model_id索引
CREATE INDEX idx_workspace_model ON agent_workspace(model_id); 