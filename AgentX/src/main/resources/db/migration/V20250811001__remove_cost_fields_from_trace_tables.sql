-- 移除Agent执行链路追踪表中的费用字段
-- 费用管理统一由usage_records表负责，遵循单一职责原则

-- 移除汇总表的总成本字段
ALTER TABLE agent_execution_summary DROP COLUMN IF EXISTS total_cost;

-- 移除详情表的步骤成本字段  
ALTER TABLE agent_execution_details DROP COLUMN IF EXISTS step_cost;

-- 更新表注释
COMMENT ON TABLE agent_execution_summary IS 'Agent执行链路汇总表，记录每次Agent执行的汇总信息（不包含费用，费用由usage_records管理）';
COMMENT ON TABLE agent_execution_details IS 'Agent执行链路详细记录表，记录每次执行的详细过程（不包含费用，费用由usage_records管理）';