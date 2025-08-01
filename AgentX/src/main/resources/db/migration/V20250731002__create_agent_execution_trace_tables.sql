-- Agent执行链路追踪系统数据表
-- 用于记录Agent执行的完整链路信息，支持问题排查和性能监控

-- Agent执行链路汇总表
CREATE TABLE agent_execution_summary (
    -- 基础标识
    id BIGSERIAL PRIMARY KEY,
    trace_id VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    agent_id VARCHAR(64) NOT NULL,
    
    -- 执行时间信息
    execution_start_time TIMESTAMP NOT NULL,
    execution_end_time TIMESTAMP,
    total_execution_time INTEGER,
    
    -- Token汇总统计（所有模型调用的总和）
    total_input_tokens INTEGER DEFAULT 0,
    total_output_tokens INTEGER DEFAULT 0,
    total_tokens INTEGER DEFAULT 0,
    
    -- 工具调用汇总
    tool_call_count INTEGER DEFAULT 0,
    total_tool_execution_time INTEGER DEFAULT 0,
    
    -- 成本汇总
    total_cost DECIMAL(10,6) DEFAULT 0,
    
    -- 执行状态
    execution_success BOOLEAN NOT NULL,
    error_phase VARCHAR(64),
    error_message TEXT,
    
    -- 时间戳
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建汇总表索引
CREATE INDEX idx_agent_exec_summary_user_time ON agent_execution_summary(user_id, execution_start_time);
CREATE INDEX idx_agent_exec_summary_session ON agent_execution_summary(session_id);
CREATE INDEX idx_agent_exec_summary_agent ON agent_execution_summary(agent_id);
CREATE INDEX idx_agent_exec_summary_trace ON agent_execution_summary(trace_id);

-- Agent执行链路详细记录表
CREATE TABLE agent_execution_details (
    -- 基础标识
    id BIGSERIAL PRIMARY KEY,
    trace_id VARCHAR(64) NOT NULL,
    sequence_no INTEGER NOT NULL,
    step_type VARCHAR(32) NOT NULL,
    
    -- 用户消息
    user_message TEXT,
    user_message_type VARCHAR(16),
    
    -- AI响应
    ai_response TEXT,
    ai_response_type VARCHAR(16),
    
    -- 模型调用详情（每次调用都记录）
    model_id VARCHAR(128),
    provider_name VARCHAR(64),
    input_tokens INTEGER,
    output_tokens INTEGER,
    model_call_time INTEGER,
    
    -- 工具调用详情
    tool_name VARCHAR(128),
    tool_request_args TEXT,
    tool_response_data TEXT,
    tool_execution_time INTEGER,
    tool_success BOOLEAN,
    
    -- 降级/平替信息（针对单次调用）
    is_fallback_used BOOLEAN DEFAULT FALSE,
    fallback_reason TEXT,
    fallback_from_model VARCHAR(128),
    fallback_to_model VARCHAR(128),
    
    -- 成本详情
    step_cost DECIMAL(10,6),
    
    -- 状态
    step_success BOOLEAN NOT NULL,
    step_error_message TEXT,
    
    -- 时间戳
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建详情表索引
CREATE INDEX idx_agent_exec_details_trace_seq ON agent_execution_details(trace_id, sequence_no);
CREATE INDEX idx_agent_exec_details_trace_type ON agent_execution_details(trace_id, step_type);
CREATE INDEX idx_agent_exec_details_tool ON agent_execution_details(tool_name);
CREATE INDEX idx_agent_exec_details_model ON agent_execution_details(model_id);

-- 添加外键约束
ALTER TABLE agent_execution_details 
ADD CONSTRAINT fk_agent_exec_details_trace 
FOREIGN KEY (trace_id) REFERENCES agent_execution_summary(trace_id);

-- 添加表注释
COMMENT ON TABLE agent_execution_summary IS 'Agent执行链路汇总表，记录每次Agent执行的汇总信息';
COMMENT ON TABLE agent_execution_details IS 'Agent执行链路详细记录表，记录每次执行的详细过程';

-- 添加重要字段注释
COMMENT ON COLUMN agent_execution_summary.trace_id IS '执行追踪ID，唯一标识一次完整执行';
COMMENT ON COLUMN agent_execution_summary.user_id IS '用户ID';
COMMENT ON COLUMN agent_execution_summary.session_id IS '会话ID';
COMMENT ON COLUMN agent_execution_summary.agent_id IS 'Agent ID (String类型UUID)';
COMMENT ON COLUMN agent_execution_summary.total_execution_time IS '总执行时间(毫秒)';
COMMENT ON COLUMN agent_execution_summary.total_tokens IS '总Token数';
COMMENT ON COLUMN agent_execution_summary.tool_call_count IS '工具调用总次数';
COMMENT ON COLUMN agent_execution_summary.total_cost IS '总成本费用';
COMMENT ON COLUMN agent_execution_summary.execution_success IS '执行是否成功';

COMMENT ON COLUMN agent_execution_details.trace_id IS '关联汇总表的追踪ID';
COMMENT ON COLUMN agent_execution_details.sequence_no IS '执行序号，同一trace_id内递增';
COMMENT ON COLUMN agent_execution_details.step_type IS '步骤类型：USER_MESSAGE, AI_RESPONSE, TOOL_CALL';
COMMENT ON COLUMN agent_execution_details.user_message IS '用户发送的消息内容';
COMMENT ON COLUMN agent_execution_details.ai_response IS '大模型回复的消息内容';
COMMENT ON COLUMN agent_execution_details.model_id IS '此次使用的模型ID';
COMMENT ON COLUMN agent_execution_details.tool_request_args IS '工具调用入参(JSON格式)';
COMMENT ON COLUMN agent_execution_details.tool_response_data IS '工具调用出参(JSON格式)';
COMMENT ON COLUMN agent_execution_details.is_fallback_used IS '是否触发了平替/降级';