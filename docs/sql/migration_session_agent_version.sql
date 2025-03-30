-- 会话表关联Agent版本迁移脚本
-- 创建于: 2025-03-23

-- 第一步: 添加新字段agent_version_id
ALTER TABLE sessions ADD COLUMN agent_id VARCHAR(36);

-- 第二步: 添加注释
COMMENT ON COLUMN sessions.agent_id IS '关联的agentId';

-- 第四步: 创建索引
CREATE INDEX idx_sessions_agent_id ON sessions(agent_id);

