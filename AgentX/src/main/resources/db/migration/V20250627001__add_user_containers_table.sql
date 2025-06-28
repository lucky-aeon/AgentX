-- 创建用户容器表
CREATE TABLE user_containers (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    type INTEGER NOT NULL,
    status INTEGER NOT NULL,
    docker_container_id VARCHAR(100),
    image VARCHAR(200) NOT NULL,
    internal_port INTEGER NOT NULL,
    external_port INTEGER,
    ip_address VARCHAR(45),
    cpu_usage DECIMAL(5,2),
    memory_usage DECIMAL(5,2),
    volume_path VARCHAR(500),
    env_config TEXT,
    container_config TEXT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);


-- 添加表注释
COMMENT ON TABLE user_containers IS '用户容器表';
COMMENT ON COLUMN user_containers.id IS '容器ID';
COMMENT ON COLUMN user_containers.name IS '容器名称';
COMMENT ON COLUMN user_containers.user_id IS '用户ID';
COMMENT ON COLUMN user_containers.type IS '容器类型: 1-用户容器, 2-审核容器';
COMMENT ON COLUMN user_containers.status IS '容器状态: 1-创建中, 2-运行中, 3-已停止, 4-错误状态, 5-删除中, 6-已删除';
COMMENT ON COLUMN user_containers.docker_container_id IS 'Docker容器ID';
COMMENT ON COLUMN user_containers.image IS '容器镜像';
COMMENT ON COLUMN user_containers.internal_port IS '内部端口';
COMMENT ON COLUMN user_containers.external_port IS '外部映射端口';
COMMENT ON COLUMN user_containers.ip_address IS '容器IP地址';
COMMENT ON COLUMN user_containers.cpu_usage IS 'CPU使用率(%)';
COMMENT ON COLUMN user_containers.memory_usage IS '内存使用率(%)';
COMMENT ON COLUMN user_containers.volume_path IS '数据卷路径';
COMMENT ON COLUMN user_containers.env_config IS '环境变量配置(JSON)';
COMMENT ON COLUMN user_containers.container_config IS '容器配置(JSON)';
COMMENT ON COLUMN user_containers.error_message IS '错误信息';
COMMENT ON COLUMN user_containers.created_at IS '创建时间';
COMMENT ON COLUMN user_containers.updated_at IS '更新时间';