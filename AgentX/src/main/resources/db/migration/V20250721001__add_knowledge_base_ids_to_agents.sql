-- 为agents表添加knowledge_base_ids字段
-- 用于存储Agent关联的知识库ID列表，支持RAG功能

ALTER TABLE agents ADD COLUMN knowledge_base_ids JSONB COMMENT '关联的知识库ID列表';

-- 添加字段注释
COMMENT ON COLUMN agents.knowledge_base_ids IS '关联的知识库ID列表，JSON数组格式，用于RAG功能';
