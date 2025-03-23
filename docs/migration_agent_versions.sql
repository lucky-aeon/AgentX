-- 为agent_versions表添加缺失的字段
-- 添加基本信息字段
ALTER TABLE agent_versions ADD COLUMN IF NOT EXISTS name VARCHAR(50);
ALTER TABLE agent_versions ADD COLUMN IF NOT EXISTS avatar VARCHAR(255);
ALTER TABLE agent_versions ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE agent_versions ADD COLUMN IF NOT EXISTS user_id VARCHAR(36);

-- 添加时间相关字段
ALTER TABLE agent_versions ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE agent_versions ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE agent_versions ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- 添加注释
COMMENT ON COLUMN agent_versions.name IS 'Agent名称';
COMMENT ON COLUMN agent_versions.avatar IS 'Agent头像URL';
COMMENT ON COLUMN agent_versions.description IS 'Agent描述';
COMMENT ON COLUMN agent_versions.user_id IS '创建者用户ID';
COMMENT ON COLUMN agent_versions.created_at IS '创建时间';
COMMENT ON COLUMN agent_versions.updated_at IS '最后更新时间';
COMMENT ON COLUMN agent_versions.deleted_at IS '删除时间（软删除）';

-- 更新已有数据，从agents表中复制数据到对应的版本记录
UPDATE agent_versions av 
SET name = a.name,
    avatar = a.avatar,
    description = a.description,
    user_id = a.user_id
FROM agents a
WHERE av.agent_id = a.id;

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_agent_versions_name ON agent_versions(name);
CREATE INDEX IF NOT EXISTS idx_agent_versions_user_id ON agent_versions(user_id); 