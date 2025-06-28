-- 添加容器最后访问时间字段，用于自动清理
ALTER TABLE user_containers ADD COLUMN last_accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 添加字段注释
COMMENT ON COLUMN user_containers.last_accessed_at IS '最后访问时间，用于自动清理判断';
