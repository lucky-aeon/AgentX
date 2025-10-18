-- 为 agents 表添加 linked_agent_ids 字段（Multi-Agent 关联）
ALTER TABLE agents
    ADD COLUMN IF NOT EXISTS linked_agent_ids JSONB;

COMMENT ON COLUMN agents.linked_agent_ids IS '关联的子Agent ID列表，JSON数组格式，用于Multi-Agent工具调用';

