-- 为消息表添加本体token数字段
-- body_token_count 字段用于优化滑动窗口策略时的上下文窗口token数计算
ALTER TABLE messages
    ADD COLUMN body_token_count INTEGER DEFAULT 0;

COMMENT ON COLUMN messages.body_token_count IS '消息本体Token数量（不含上下文）';
