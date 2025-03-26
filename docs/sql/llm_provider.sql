-- 服务商配置表
CREATE TABLE llm_provider_configs (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '配置名称',
    provider_type VARCHAR(50) NOT NULL COMMENT '服务商类型',
    encrypted_config TEXT NOT NULL COMMENT '加密的服务商配置',
    is_official BOOLEAN DEFAULT false COMMENT '是否官方配置',
    user_id BIGINT COMMENT '用户ID，官方配置为空',
    status SMALLINT DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false
);

-- 创建索引
CREATE INDEX idx_llm_provider_configs_user_id ON llm_provider_configs(user_id) WHERE is_deleted = false;
CREATE INDEX idx_llm_provider_configs_type ON llm_provider_configs(provider_type) WHERE is_deleted = false; 