-- 为usage_records表添加业务信息字段
-- 作者: Claude Code
-- 创建时间: 2025-08-03
-- 描述: 在用量记录创建时固化业务信息，避免历史记录受商品变更影响

-- 先添加字段
ALTER TABLE usage_records
    ADD COLUMN service_name VARCHAR(255),
    ADD COLUMN service_type VARCHAR(100),
    ADD COLUMN service_description TEXT,
    ADD COLUMN pricing_rule TEXT,
    ADD COLUMN related_entity_name VARCHAR(255);

-- 再单独添加注释
COMMENT ON COLUMN usage_records.service_name IS '服务名称（如：GPT-4 模型调用）';
COMMENT ON COLUMN usage_records.service_type IS '服务类型（如：模型服务）';
COMMENT ON COLUMN usage_records.service_description IS '服务描述';
COMMENT ON COLUMN usage_records.pricing_rule IS '定价规则说明（如：输入 ¥0.002/1K tokens，输出 ¥0.006/1K tokens）';
COMMENT ON COLUMN usage_records.related_entity_name IS '关联实体名称（如：具体的模型名称或Agent名称）';
