-- 创建任务表
CREATE TABLE IF NOT EXISTS agent_tasks (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    parent_task_id VARCHAR(36),
    task_name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    progress INT DEFAULT 0,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delete_at TIMESTAMP,
    task_result TEXT
);

-- 创建索引
CREATE INDEX idx_tasks_session_id ON agent_tasks(session_id);
CREATE INDEX idx_tasks_user_id ON agent_tasks(user_id);

-- 添加表和列注释
COMMENT ON TABLE agent_tasks IS '代理任务表，用于记录AI代理执行的任务';
COMMENT ON COLUMN agent_tasks.id IS '任务唯一ID';
COMMENT ON COLUMN agent_tasks.session_id IS '所属会话ID';
COMMENT ON COLUMN agent_tasks.user_id IS '用户ID';
COMMENT ON COLUMN agent_tasks.parent_task_id IS '父任务ID，用于任务嵌套';
COMMENT ON COLUMN agent_tasks.task_name IS '任务名称';
COMMENT ON COLUMN agent_tasks.description IS '任务详细描述';
COMMENT ON COLUMN agent_tasks.status IS '任务状态，如等待中、进行中、已完成、已失败等';
COMMENT ON COLUMN agent_tasks.progress IS '任务进度，范围0-100';
COMMENT ON COLUMN agent_tasks.start_time IS '任务开始执行时间';
COMMENT ON COLUMN agent_tasks.end_time IS '任务结束时间';
COMMENT ON COLUMN agent_tasks.created_at IS '记录创建时间';
COMMENT ON COLUMN agent_tasks.updated_at IS '记录更新时间';
COMMENT ON COLUMN agent_tasks.delete_at IS '逻辑删除时间'; 
COMMENT ON COLUMN agent_tasks.task_result IS '任务结果';