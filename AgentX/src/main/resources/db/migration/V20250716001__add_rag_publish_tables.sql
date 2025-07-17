-- RAG版本表（完整快照）
CREATE TABLE rag_versions (
    id VARCHAR(36) PRIMARY KEY,
    
    -- 基本信息（快照时的数据）
    name VARCHAR(255) NOT NULL,                 -- 快照时的名称
    icon VARCHAR(255),                          -- 快照时的图标
    description TEXT,                           -- 快照时的描述
    user_id VARCHAR(36) NOT NULL,               -- 创建者
    
    -- 版本信息
    version VARCHAR(50) NOT NULL,               -- 版本号 (如 "1.0.0")
    change_log TEXT,                            -- 更新日志
    labels JSONB,                               -- 标签
    
    -- 原始数据集信息（仅用于标识，不依赖）
    original_rag_id VARCHAR(36) NOT NULL,       -- 原始RAG数据集ID（仅标识用）
    original_rag_name VARCHAR(255),             -- 原始RAG名称（快照时）
    
    -- 快照统计信息
    file_count INTEGER DEFAULT 0,               -- 文件数量
    total_size BIGINT DEFAULT 0,                -- 总大小
    document_count INTEGER DEFAULT 0,           -- 文档单元数量
    
    -- 发布状态（需要审核）
    publish_status INTEGER DEFAULT 1,           -- 1:审核中, 2:已发布, 3:拒绝, 4:已下架
    reject_reason TEXT,                         -- 审核拒绝原因
    review_time TIMESTAMP,                      -- 审核时间
    published_at TIMESTAMP,                     -- 发布时间
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- RAG版本文件表（文件快照）
CREATE TABLE rag_version_files (
    id VARCHAR(36) PRIMARY KEY,
    rag_version_id VARCHAR(36) NOT NULL,        -- 关联RAG版本
    
    -- 文件信息快照
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

-- RAG版本文档单元表（文档内容快照）
CREATE TABLE rag_version_documents (
    id VARCHAR(36) PRIMARY KEY,
    rag_version_id VARCHAR(36) NOT NULL,        -- 关联RAG版本
    rag_version_file_id VARCHAR(36) NOT NULL,   -- 关联版本文件
    
    -- 文档内容快照
    original_document_id VARCHAR(36),           -- 原始文档单元ID（仅标识）
    content TEXT NOT NULL,                      -- 文档内容
    page INTEGER,                               -- 页码
    
    -- 向量数据存储在向量数据库中，使用 rag_version_id 作为命名空间
    vector_id VARCHAR(36),                      -- 向量ID
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- RAG安装表（用户安装记录）
CREATE TABLE user_rags (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    rag_version_id VARCHAR(36) NOT NULL,        -- 关联的RAG版本快照
    
    -- 安装时的信息
    name VARCHAR(255) NOT NULL,                 -- 安装时的名称
    description TEXT,                           -- 安装时的描述
    icon VARCHAR(255),                          -- 安装时的图标
    version VARCHAR(50) NOT NULL,               -- 版本号
    
    -- 安装状态
    is_active BOOLEAN DEFAULT TRUE,             -- 是否激活
    installed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    
    UNIQUE (user_id, rag_version_id)
);


-- 添加表注释
COMMENT ON TABLE rag_versions IS 'RAG版本表（完整快照）';
COMMENT ON TABLE rag_version_files IS 'RAG版本文件表（文件快照）';
COMMENT ON TABLE rag_version_documents IS 'RAG版本文档单元表（文档内容快照）';
COMMENT ON TABLE user_rags IS '用户安装的RAG表';

-- 添加字段注释
-- rag_versions表字段注释
COMMENT ON COLUMN rag_versions.id IS '主键ID';
COMMENT ON COLUMN rag_versions.name IS '快照时的名称';
COMMENT ON COLUMN rag_versions.icon IS '快照时的图标';
COMMENT ON COLUMN rag_versions.description IS '快照时的描述';
COMMENT ON COLUMN rag_versions.user_id IS '创建者用户ID';
COMMENT ON COLUMN rag_versions.version IS '版本号 (如 "1.0.0")';
COMMENT ON COLUMN rag_versions.change_log IS '更新日志';
COMMENT ON COLUMN rag_versions.labels IS '标签（JSON格式）';
COMMENT ON COLUMN rag_versions.original_rag_id IS '原始RAG数据集ID（仅标识用）';
COMMENT ON COLUMN rag_versions.original_rag_name IS '原始RAG名称（快照时）';
COMMENT ON COLUMN rag_versions.file_count IS '文件数量';
COMMENT ON COLUMN rag_versions.total_size IS '总大小（字节）';
COMMENT ON COLUMN rag_versions.document_count IS '文档单元数量';
COMMENT ON COLUMN rag_versions.publish_status IS '发布状态 1:审核中, 2:已发布, 3:拒绝, 4:已下架';
COMMENT ON COLUMN rag_versions.reject_reason IS '审核拒绝原因';
COMMENT ON COLUMN rag_versions.review_time IS '审核时间';
COMMENT ON COLUMN rag_versions.published_at IS '发布时间';
COMMENT ON COLUMN rag_versions.created_at IS '创建时间';
COMMENT ON COLUMN rag_versions.updated_at IS '更新时间';
COMMENT ON COLUMN rag_versions.deleted_at IS '删除时间（软删除）';

-- rag_version_files表字段注释
COMMENT ON COLUMN rag_version_files.id IS '主键ID';
COMMENT ON COLUMN rag_version_files.rag_version_id IS '关联RAG版本ID';
COMMENT ON COLUMN rag_version_files.original_file_id IS '原始文件ID（仅标识）';
COMMENT ON COLUMN rag_version_files.file_name IS '文件名';
COMMENT ON COLUMN rag_version_files.file_size IS '文件大小（字节）';
COMMENT ON COLUMN rag_version_files.file_type IS '文件类型';
COMMENT ON COLUMN rag_version_files.file_path IS '文件存储路径';
COMMENT ON COLUMN rag_version_files.process_status IS '处理状态';
COMMENT ON COLUMN rag_version_files.embedding_status IS '向量化状态';
COMMENT ON COLUMN rag_version_files.created_at IS '创建时间';
COMMENT ON COLUMN rag_version_files.updated_at IS '更新时间';
COMMENT ON COLUMN rag_version_files.deleted_at IS '删除时间（软删除）';

-- rag_version_documents表字段注释
COMMENT ON COLUMN rag_version_documents.id IS '主键ID';
COMMENT ON COLUMN rag_version_documents.rag_version_id IS '关联RAG版本ID';
COMMENT ON COLUMN rag_version_documents.rag_version_file_id IS '关联版本文件ID';
COMMENT ON COLUMN rag_version_documents.original_document_id IS '原始文档单元ID（仅标识）';
COMMENT ON COLUMN rag_version_documents.content IS '文档内容';
COMMENT ON COLUMN rag_version_documents.page IS '页码';
COMMENT ON COLUMN rag_version_documents.vector_id IS '向量ID';
COMMENT ON COLUMN rag_version_documents.created_at IS '创建时间';
COMMENT ON COLUMN rag_version_documents.updated_at IS '更新时间';
COMMENT ON COLUMN rag_version_documents.deleted_at IS '删除时间（软删除）';

-- user_rags表字段注释
COMMENT ON COLUMN user_rags.id IS '主键ID';
COMMENT ON COLUMN user_rags.user_id IS '用户ID';
COMMENT ON COLUMN user_rags.rag_version_id IS '关联的RAG版本快照ID';
COMMENT ON COLUMN user_rags.name IS '安装时的名称';
COMMENT ON COLUMN user_rags.description IS '安装时的描述';
COMMENT ON COLUMN user_rags.icon IS '安装时的图标';
COMMENT ON COLUMN user_rags.version IS '版本号';
COMMENT ON COLUMN user_rags.is_active IS '是否激活';
COMMENT ON COLUMN user_rags.installed_at IS '安装时间';
COMMENT ON COLUMN user_rags.created_at IS '创建时间';
COMMENT ON COLUMN user_rags.updated_at IS '更新时间';
COMMENT ON COLUMN user_rags.deleted_at IS '删除时间（软删除）';