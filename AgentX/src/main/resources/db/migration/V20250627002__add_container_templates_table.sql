-- 创建容器模板表
CREATE TABLE container_templates (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    type VARCHAR(50) NOT NULL,
    image VARCHAR(200) NOT NULL,
    image_tag VARCHAR(50),
    internal_port INTEGER NOT NULL,
    cpu_limit DECIMAL(4,2) NOT NULL,
    memory_limit INTEGER NOT NULL,
    environment TEXT,
    volume_mount_path VARCHAR(500),
    command TEXT,
    network_mode VARCHAR(50),
    restart_policy VARCHAR(50),
    health_check TEXT,
    resource_config TEXT,
    enabled BOOLEAN NOT NULL DEFAULT true,
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_by VARCHAR(36),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);

-- 创建索引
CREATE INDEX idx_container_templates_type ON container_templates(type);
CREATE INDEX idx_container_templates_enabled ON container_templates(enabled);
CREATE INDEX idx_container_templates_is_default ON container_templates(is_default);
CREATE INDEX idx_container_templates_created_by ON container_templates(created_by);
CREATE INDEX idx_container_templates_sort_order ON container_templates(sort_order);
CREATE INDEX idx_container_templates_created_at ON container_templates(created_at);

-- 创建复合索引：类型+启用状态+默认状态
CREATE INDEX idx_container_templates_type_enabled_default ON container_templates(type, enabled, is_default);

-- 确保每个类型只有一个默认模板
CREATE UNIQUE INDEX idx_container_templates_unique_default ON container_templates(type) WHERE is_default = true;

-- 添加表注释
COMMENT ON TABLE container_templates IS '容器模板表';
COMMENT ON COLUMN container_templates.id IS '模板ID';
COMMENT ON COLUMN container_templates.name IS '模板名称';
COMMENT ON COLUMN container_templates.description IS '模板描述';
COMMENT ON COLUMN container_templates.type IS '模板类型(mcp-gateway等)';
COMMENT ON COLUMN container_templates.image IS '容器镜像名称';
COMMENT ON COLUMN container_templates.image_tag IS '镜像版本标签';
COMMENT ON COLUMN container_templates.internal_port IS '容器内部端口';
COMMENT ON COLUMN container_templates.cpu_limit IS 'CPU限制(核数)';
COMMENT ON COLUMN container_templates.memory_limit IS '内存限制(MB)';
COMMENT ON COLUMN container_templates.environment IS '环境变量配置(JSON格式)';
COMMENT ON COLUMN container_templates.volume_mount_path IS '数据卷挂载路径';
COMMENT ON COLUMN container_templates.command IS '启动命令(JSON数组格式)';
COMMENT ON COLUMN container_templates.network_mode IS '网络模式';
COMMENT ON COLUMN container_templates.restart_policy IS '重启策略';
COMMENT ON COLUMN container_templates.health_check IS '健康检查配置(JSON格式)';
COMMENT ON COLUMN container_templates.resource_config IS '资源配置(JSON格式)';
COMMENT ON COLUMN container_templates.enabled IS '是否启用';
COMMENT ON COLUMN container_templates.is_default IS '是否为默认模板';
COMMENT ON COLUMN container_templates.created_by IS '创建者用户ID';
COMMENT ON COLUMN container_templates.sort_order IS '排序权重';
COMMENT ON COLUMN container_templates.created_at IS '创建时间';
COMMENT ON COLUMN container_templates.updated_at IS '更新时间';
COMMENT ON COLUMN container_templates.deleted_at IS '删除时间';

-- 插入默认的MCP网关模板
INSERT INTO container_templates (
    id, name, description, type, image, image_tag, internal_port, 
    cpu_limit, memory_limit, volume_mount_path, network_mode, 
    restart_policy, enabled, is_default, created_by, sort_order
) VALUES (
    'default-mcp-gateway-template',
    'MCP网关默认模板',
    '用于创建用户MCP网关容器的默认模板，提供工具部署和Agent对话功能',
    'mcp-gateway',
    'ghcr.io/lucky-aeon/mcp-gateway',
    'latest',
    8080,
    1.0,
    512,
    '/app/data',
    'bridge',
    'unless-stopped',
    true,
    true,
    'SYSTEM',
    0
);