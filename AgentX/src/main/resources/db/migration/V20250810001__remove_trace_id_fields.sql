-- 移除 trace_id 字段的迁移脚本
-- 将追踪系统改为基于 session_id 的设计

-- 1. 移除 agent_execution_summary 表的 trace_id 相关索引和字段
DROP INDEX IF EXISTS idx_agent_exec_summary_trace;
ALTER TABLE agent_execution_summary DROP COLUMN IF EXISTS trace_id;

-- 2. 移除 agent_execution_details 表的 trace_id 相关索引和字段
DROP INDEX IF EXISTS idx_agent_exec_details_trace_seq;
DROP INDEX IF EXISTS idx_agent_exec_details_trace_type;

-- 为 agent_execution_details 添加 session_id 字段（如果不存在）
ALTER TABLE agent_execution_details ADD COLUMN IF NOT EXISTS session_id VARCHAR(64) NOT NULL DEFAULT '';

-- 3. 如果有历史数据，从 agent_execution_summary 获取 session_id 更新 agent_execution_details
-- 注意：这个步骤假设现有数据中 trace_id 和 session_id 有对应关系
UPDATE agent_execution_details 
SET session_id = (
    SELECT s.session_id 
    FROM agent_execution_summary s 
    WHERE s.trace_id = agent_execution_details.trace_id
)
WHERE session_id = '' AND trace_id IS NOT NULL;

-- 4. 移除 agent_execution_details 表的 trace_id 字段
ALTER TABLE agent_execution_details DROP COLUMN IF EXISTS trace_id;

-- 5. 创建基于 session_id 的新索引
CREATE INDEX idx_agent_exec_details_session_seq ON agent_execution_details(session_id, sequence_no);
CREATE INDEX idx_agent_exec_details_session_type ON agent_execution_details(session_id, message_type);

-- 6. 确保 agent_execution_summary 表的 session_id 唯一性
-- 删除重复记录，保留最新的记录
DELETE FROM agent_execution_summary 
WHERE id NOT IN (
    SELECT MAX(id) 
    FROM agent_execution_summary 
    GROUP BY session_id
);

-- 7. 为 session_id 添加唯一约束
ALTER TABLE agent_execution_summary ADD CONSTRAINT uk_agent_exec_summary_session_id UNIQUE (session_id);

-- 8. 更新字段注释
COMMENT ON COLUMN agent_execution_summary.session_id IS '会话ID，作为追踪的唯一标识';
COMMENT ON COLUMN agent_execution_details.session_id IS '关联汇总表的会话ID';
COMMENT ON COLUMN agent_execution_details.sequence_no IS '执行序号，同一session_id内递增';