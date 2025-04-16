-- 修改消息表，添加消息类型字段
ALTER TABLE messages 
ADD COLUMN message_type VARCHAR(50) DEFAULT 'TEXT';  -- 消息类型字段，默认为文本类型 

-- 添加列注释
COMMENT ON COLUMN messages.message_type IS '消息类型，如文本、任务状态更新、代码片段等'; 