-- Token超限处理策略相关数据库扩展

-- 在context表中添加字段
ALTER TABLE contexts ADD COLUMN overflow_strategy VARCHAR(20) DEFAULT 'NONE' COMMENT 'Token超限处理策略: NONE, SLIDING_WINDOW, SUMMARIZE';
ALTER TABLE contexts ADD COLUMN strategy_config JSON COMMENT 'Token策略配置参数(JSON格式)';
ALTER TABLE contexts ADD COLUMN summary TEXT COMMENT '对话摘要内容';

-- 在agents表中添加字段
ALTER TABLE agents ADD COLUMN token_overflow_config JSON COMMENT 'Token超限配置(JSON格式)';

-- 在agent_versions表中添加字段
ALTER TABLE agent_versions ADD COLUMN token_overflow_config JSON COMMENT 'Token超限配置(JSON格式)'; 