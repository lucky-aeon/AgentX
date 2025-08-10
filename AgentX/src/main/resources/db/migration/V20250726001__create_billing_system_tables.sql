-- 计费系统数据库表创建脚本 (PostgreSQL)
-- 作者: Claude Code
-- 创建时间: 2025-07-26
-- 描述: 为AgentX计费系统创建Products、Rules、Accounts、UsageRecords表

-- 如果表存在则删除（开发环境重建用）
DROP TABLE IF EXISTS usage_records;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS rules;

-- 1. 创建规则表 (rules)
-- 存储计费规则信息，定义不同的计费策略
CREATE TABLE rules (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    handler_key VARCHAR(100) NOT NULL,
    description TEXT,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- 2. 创建商品表 (products)  
-- 存储计费商品信息，关联规则和价格配置
CREATE TABLE products (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    service_id VARCHAR(100) NOT NULL,
    rule_id VARCHAR(64) NOT NULL,
    pricing_config JSONB,
    status INTEGER DEFAULT 1,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. 创建账户表 (accounts)
-- 存储用户账户余额和消费信息
CREATE TABLE accounts (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    balance DECIMAL(20,8) DEFAULT 0.00000000,
    credit DECIMAL(20,8) DEFAULT 0.00000000,
    total_consumed DECIMAL(20,8) DEFAULT 0.00000000,
    last_transaction_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. 创建使用记录表 (usage_records)
-- 存储用户的计费使用记录
CREATE TABLE usage_records (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    quantity_data JSONB,
    cost DECIMAL(20,8) NOT NULL,
    request_id VARCHAR(255) NOT NULL,
    billed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- 添加表注释
COMMENT ON TABLE rules IS '计费规则表，存储不同的计费策略配置';
COMMENT ON TABLE products IS '计费商品表，存储可计费的服务和产品信息';
COMMENT ON TABLE accounts IS '用户账户表，存储用户余额和消费记录';
COMMENT ON TABLE usage_records IS '使用记录表，存储用户的具体消费记录';

-- 添加列注释
COMMENT ON COLUMN rules.name IS '规则名称';
COMMENT ON COLUMN rules.handler_key IS '处理器标识，对应策略枚举';
COMMENT ON COLUMN rules.description IS '规则描述';

COMMENT ON COLUMN products.name IS '商品名称';
COMMENT ON COLUMN products.type IS '计费类型：MODEL_USAGE(模型调用), AGENT_CREATION(Agent创建), AGENT_USAGE(Agent使用), API_CALL(API调用), STORAGE_USAGE(存储使用)';
COMMENT ON COLUMN products.service_id IS '业务服务标识';
COMMENT ON COLUMN products.rule_id IS '关联的规则ID';
COMMENT ON COLUMN products.pricing_config IS '价格配置（JSONB格式）';
COMMENT ON COLUMN products.status IS '状态：1-激活，0-禁用';

COMMENT ON COLUMN accounts.user_id IS '用户ID';
COMMENT ON COLUMN accounts.balance IS '账户余额';
COMMENT ON COLUMN accounts.credit IS '信用额度';
COMMENT ON COLUMN accounts.total_consumed IS '总消费金额';
COMMENT ON COLUMN accounts.last_transaction_at IS '最后交易时间';

COMMENT ON COLUMN usage_records.user_id IS '用户ID';
COMMENT ON COLUMN usage_records.product_id IS '商品ID';
COMMENT ON COLUMN usage_records.quantity_data IS '使用量数据（JSONB格式）';
COMMENT ON COLUMN usage_records.cost IS '本次消费金额';
COMMENT ON COLUMN usage_records.request_id IS '请求ID（幂等性保证）';
COMMENT ON COLUMN usage_records.billed_at IS '计费时间';

-- 插入初始数据：基础计费规则
INSERT INTO rules (id, name, handler_key, description) VALUES 
('rule-model-token', '模型Token计费规则', 'MODEL_TOKEN_STRATEGY', '按输入输出Token数量计费，适用于大语言模型调用'),
('rule-per-unit', '按次计费规则', 'PER_UNIT_STRATEGY', '按使用次数固定计费，适用于Agent创建、API调用等'),
('rule-per-time', '按时长计费规则', 'PER_TIME_STRATEGY', '按使用时长计费，适用于资源占用类服务');


-- 注释说明：
-- MODEL_USAGE类型的service_id应该是models表中的主键ID（如1,2,3等）
-- AGENT_CREATION类型使用固定标识'agent_creation'，因为不对应具体业务记录（已预留配置）
-- 实际部署时，需要根据models表的真实ID更新这些配置
-- Agent创建计费已完整预留：type='AGENT_CREATION', service_id='agent_creation', cost_per_unit=10.0