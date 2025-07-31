-- 为用户安装的RAG创建快照表，实现完全数据隔离
-- Migration: V20250722_001__add_user_rag_snapshot_tables.sql
-- Description: 创建用户RAG文件和文档快照表，确保用户安装的SNAPSHOT类型RAG有独立数据副本

-- 用户RAG文件快照表
CREATE TABLE user_rag_files (
    id VARCHAR(36) PRIMARY KEY,
    user_rag_id VARCHAR(36) NOT NULL,           -- 关联user_rags表
    
    -- 文件信息快照（从rag_version_files复制）
    original_file_id VARCHAR(36) NOT NULL,      -- 原始文件ID（仅标识）
    file_name VARCHAR(255) NOT NULL,            -- 文件名
    file_size BIGINT DEFAULT 0,                 -- 文件大小
    file_type VARCHAR(50),                      -- 文件类型
    file_path VARCHAR(500),                     -- 文件存储路径
    
    -- 处理状态快照
    process_status INTEGER,                     -- 处理状态
    embedding_status INTEGER,                   -- 向量化状态
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 用户RAG文档快照表  
CREATE TABLE user_rag_documents (
    id VARCHAR(36) PRIMARY KEY,
    user_rag_id VARCHAR(36) NOT NULL,           -- 关联user_rags表
    user_rag_file_id VARCHAR(36) NOT NULL,      -- 关联user_rag_files表
    
    -- 文档内容快照（从rag_version_documents复制）
    original_document_id VARCHAR(36),           -- 原始文档单元ID（仅标识）
    content TEXT NOT NULL,                      -- 文档内容
    page INTEGER,                               -- 页码
    
    -- 向量数据存储在向量数据库中，使用 user_rag_id 作为命名空间
    vector_id VARCHAR(36),                      -- 向量ID
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);


-- 添加表注释
COMMENT ON TABLE user_rag_files IS '用户RAG文件快照表 - 用于SNAPSHOT类型RAG的完全数据隔离';
COMMENT ON TABLE user_rag_documents IS '用户RAG文档快照表 - 用于SNAPSHOT类型RAG的完全数据隔离';

-- 添加字段注释
-- user_rag_files表字段注释
COMMENT ON COLUMN user_rag_files.id IS '主键ID';
COMMENT ON COLUMN user_rag_files.user_rag_id IS '关联user_rags表的ID';
COMMENT ON COLUMN user_rag_files.original_file_id IS '原始文件ID（仅用于标识，不依赖）';
COMMENT ON COLUMN user_rag_files.file_name IS '文件名（快照）';
COMMENT ON COLUMN user_rag_files.file_size IS '文件大小（字节）';
COMMENT ON COLUMN user_rag_files.file_type IS '文件类型';
COMMENT ON COLUMN user_rag_files.file_path IS '文件存储路径';
COMMENT ON COLUMN user_rag_files.process_status IS '处理状态（快照）';
COMMENT ON COLUMN user_rag_files.embedding_status IS '向量化状态（快照）';

-- user_rag_documents表字段注释
COMMENT ON COLUMN user_rag_documents.id IS '主键ID';
COMMENT ON COLUMN user_rag_documents.user_rag_id IS '关联user_rags表的ID';
COMMENT ON COLUMN user_rag_documents.user_rag_file_id IS '关联user_rag_files表的ID';
COMMENT ON COLUMN user_rag_documents.original_document_id IS '原始文档单元ID（仅用于标识，不依赖）';
COMMENT ON COLUMN user_rag_documents.content IS '文档内容（快照）';
COMMENT ON COLUMN user_rag_documents.page IS '页码';
COMMENT ON COLUMN user_rag_documents.vector_id IS '向量ID（在向量数据库中的ID）';