-- 为 agent_versions 表添加 linked_agent_ids 字段（Multi-Agent 关联）
ALTER TABLE agent_versions
    ADD COLUMN IF NOT EXISTS linked_agent_ids JSONB;

COMMENT ON COLUMN agent_versions.linked_agent_ids IS '关联的子Agent ID列表，JSON数组格式，用于Multi-Agent工具调用（版本冻结）';

