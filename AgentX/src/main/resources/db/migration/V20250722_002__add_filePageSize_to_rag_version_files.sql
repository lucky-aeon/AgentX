-- 为 rag_version_files 表添加 filePageSize 字段
-- 用于保存文件的页数信息到版本快照中

ALTER TABLE rag_version_files ADD COLUMN file_page_size INTEGER;

-- 添加注释
COMMENT ON COLUMN rag_version_files.file_page_size IS '文件页数';