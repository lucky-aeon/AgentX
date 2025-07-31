-- 为用户RAG文件快照表添加文件页数字段
-- Migration: V20250722_002__add_file_page_size_to_user_rag_files.sql
-- Description: 添加file_page_size字段到user_rag_files表，完善文件信息快照

-- 添加文件页数字段
ALTER TABLE user_rag_files ADD COLUMN file_page_size INTEGER DEFAULT 0;

-- 添加字段注释
COMMENT ON COLUMN user_rag_files.file_page_size IS '文件页数（快照）';

-- 如果有现有数据，可以尝试从原始文件中同步页数信息
-- 注意：这个更新操作是可选的，如果原始文件表结构不同可以跳过
UPDATE user_rag_files urf 
SET file_page_size = (
    SELECT COALESCE(fd.file_page_size, 0)
    FROM file_detail fd 
    WHERE fd.id = urf.original_file_id
)
WHERE urf.file_page_size IS NULL OR urf.file_page_size = 0;