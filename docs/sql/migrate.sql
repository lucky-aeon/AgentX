ALTER TABLE messages
    ADD COLUMN body_token_count INTEGER DEFAULT 0;

COMMENT ON COLUMN messages.body_token_count IS '消息本体Token数量（不含上下文）';
