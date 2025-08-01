-- ============================================
-- AgentX RAG相关数据表结构
-- 创建时间: 2025-01-29
-- 说明: RAG知识库系统相关表结构定义
-- ============================================

-- 设置字符集和排序规则
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. RAG知识库数据集表
-- ============================================
CREATE TABLE `ai_rag_qa_dataset` (
    `id` VARCHAR(36) NOT NULL COMMENT '数据集ID，UUID',
    `name` VARCHAR(255) NOT NULL COMMENT '数据集名称',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '数据集图标',
    `description` TEXT COMMENT '数据集说明',
    `user_id` VARCHAR(36) NOT NULL COMMENT '用户ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` TIMESTAMP NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG知识库数据集表';

-- ============================================
-- 2. 文件详情表
-- ============================================
CREATE TABLE `file_detail` (
    `id` VARCHAR(36) NOT NULL COMMENT '文件ID，UUID',
    `url` VARCHAR(500) DEFAULT NULL COMMENT '文件访问地址',
    `size` BIGINT DEFAULT NULL COMMENT '文件大小，单位字节',
    `filename` VARCHAR(255) DEFAULT NULL COMMENT '文件名称',
    `original_filename` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
    `base_path` VARCHAR(255) DEFAULT NULL COMMENT '基础存储路径',
    `path` VARCHAR(500) DEFAULT NULL COMMENT '存储路径',
    `ext` VARCHAR(10) DEFAULT NULL COMMENT '文件扩展名',
    `content_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME类型',
    `platform` VARCHAR(50) DEFAULT NULL COMMENT '存储平台',
    `th_url` VARCHAR(500) DEFAULT NULL COMMENT '缩略图访问路径',
    `th_filename` VARCHAR(255) DEFAULT NULL COMMENT '缩略图名称',
    `th_size` BIGINT DEFAULT NULL COMMENT '缩略图大小，单位字节',
    `th_content_type` VARCHAR(100) DEFAULT NULL COMMENT '缩略图MIME类型',
    `object_id` VARCHAR(36) DEFAULT NULL COMMENT '文件所属对象ID',
    `object_type` VARCHAR(50) DEFAULT NULL COMMENT '文件所属对象类型',
    `metadata` TEXT COMMENT '文件元数据',
    `user_metadata` TEXT COMMENT '文件用户元数据',
    `th_metadata` TEXT COMMENT '缩略图元数据',
    `th_user_metadata` TEXT COMMENT '缩略图用户元数据',
    `attr` TEXT COMMENT '附加属性',
    `file_acl` VARCHAR(255) DEFAULT NULL COMMENT '文件ACL',
    `th_file_acl` VARCHAR(255) DEFAULT NULL COMMENT '缩略图文件ACL',
    `hash_info` VARCHAR(255) DEFAULT NULL COMMENT '哈希信息',
    `upload_id` VARCHAR(100) DEFAULT NULL COMMENT '上传ID，仅在手动分片上传时使用',
    `upload_status` INTEGER DEFAULT NULL COMMENT '上传状态，1：初始化完成，2：上传完成',
    `user_id` VARCHAR(36) NOT NULL COMMENT '用户ID',
    `data_set_id` VARCHAR(36) NOT NULL COMMENT '数据集ID',
    `file_page_size` INTEGER DEFAULT NULL COMMENT '总页数',
    `processing_status` INTEGER DEFAULT NULL COMMENT '文件处理状态（0:已上传,1:OCR处理中,2:OCR完成,3:向量化中,4:完成,5:OCR失败,6:向量化失败）',
    `current_ocr_page_number` INTEGER DEFAULT NULL COMMENT '当前OCR处理页数',
    `current_embedding_page_number` INTEGER DEFAULT NULL COMMENT '当前向量化处理页数',
    `ocr_process_progress` DOUBLE DEFAULT NULL COMMENT 'OCR处理进度百分比',
    `embedding_process_progress` DOUBLE DEFAULT NULL COMMENT '向量化处理进度百分比',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` TIMESTAMP NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_data_set_id` (`data_set_id`),
    KEY `idx_processing_status` (`processing_status`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件详情表';

-- ============================================
-- 3. 文档单元表
-- ============================================
CREATE TABLE `document_unit` (
    `id` VARCHAR(36) NOT NULL COMMENT '主键，UUID',
    `file_id` VARCHAR(36) NOT NULL COMMENT '文件ID',
    `page` INTEGER DEFAULT NULL COMMENT '页码',
    `content` TEXT COMMENT '当前页内容',
    `is_vector` BOOLEAN DEFAULT FALSE COMMENT '是否进行向量化',
    `is_ocr` BOOLEAN DEFAULT FALSE COMMENT 'OCR识别状态',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` TIMESTAMP NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_file_id` (`file_id`),
    KEY `idx_page` (`page`),
    KEY `idx_is_vector` (`is_vector`),
    KEY `idx_is_ocr` (`is_ocr`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档单元表';

-- ============================================
-- 4. RAG版本表（版本快照）
-- ============================================
CREATE TABLE `rag_versions` (
    `id` VARCHAR(36) NOT NULL COMMENT '版本ID，UUID',
    `name` VARCHAR(255) NOT NULL COMMENT '快照时的名称',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '快照时的图标',
    `description` TEXT COMMENT '快照时的描述',
    `user_id` VARCHAR(36) NOT NULL COMMENT '创建者ID',
    `version` VARCHAR(50) NOT NULL COMMENT '版本号（如 "1.0.0"）',
    `change_log` TEXT COMMENT '更新日志',
    `labels` JSON COMMENT '标签列表（JSON格式）',
    `original_rag_id` VARCHAR(36) NOT NULL COMMENT '原始RAG数据集ID（仅标识用）',
    `original_rag_name` VARCHAR(255) DEFAULT NULL COMMENT '原始RAG名称（快照时）',
    `file_count` INTEGER DEFAULT 0 COMMENT '文件数量',
    `total_size` BIGINT DEFAULT 0 COMMENT '总大小（字节）',
    `document_count` INTEGER DEFAULT 0 COMMENT '文档单元数量',
    `publish_status` INTEGER DEFAULT 1 COMMENT '发布状态：1审核中,2已发布,3拒绝,4已下架',
    `reject_reason` TEXT COMMENT '审核拒绝原因',
    `review_time` TIMESTAMP NULL DEFAULT NULL COMMENT '审核时间',
    `published_at` TIMESTAMP NULL DEFAULT NULL COMMENT '发布时间',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` TIMESTAMP NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_original_rag_id` (`original_rag_id`),
    KEY `idx_version` (`version`),
    KEY `idx_publish_status` (`publish_status`),
    KEY `idx_published_at` (`published_at`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG版本表（版本快照）';

-- ============================================
-- 5. RAG版本文件表（文件快照）
-- ============================================
CREATE TABLE `rag_version_files` (
    `id` VARCHAR(36) NOT NULL COMMENT '文件ID，UUID',
    `rag_version_id` VARCHAR(36) NOT NULL COMMENT '关联的RAG版本ID',
    `original_file_id` VARCHAR(36) DEFAULT NULL COMMENT '原始文件ID（仅标识）',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    `file_page_size` INTEGER DEFAULT NULL COMMENT '文件页数',
    `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件类型',
    `file_path` VARCHAR(500) DEFAULT NULL COMMENT '文件存储路径',
    `process_status` INTEGER DEFAULT NULL COMMENT '处理状态',
    `embedding_status` INTEGER DEFAULT NULL COMMENT '向量化状态',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` TIMESTAMP NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_rag_version_id` (`rag_version_id`),
    KEY `idx_original_file_id` (`original_file_id`),
    KEY `idx_file_name` (`file_name`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG版本文件表（文件快照）';

-- ============================================
-- 6. RAG版本文档表（文档内容快照）
-- ============================================
CREATE TABLE `rag_version_documents` (
    `id` VARCHAR(36) NOT NULL COMMENT '文档单元ID，UUID',
    `rag_version_id` VARCHAR(36) NOT NULL COMMENT '关联的RAG版本ID',
    `rag_version_file_id` VARCHAR(36) DEFAULT NULL COMMENT '关联的版本文件ID',
    `original_document_id` VARCHAR(36) DEFAULT NULL COMMENT '原始文档单元ID（仅标识）',
    `content` TEXT COMMENT '文档内容',
    `page` INTEGER DEFAULT NULL COMMENT '页码',
    `vector_id` VARCHAR(100) DEFAULT NULL COMMENT '向量ID（在向量数据库中的ID）',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` TIMESTAMP NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_rag_version_id` (`rag_version_id`),
    KEY `idx_rag_version_file_id` (`rag_version_file_id`),
    KEY `idx_original_document_id` (`original_document_id`),
    KEY `idx_page` (`page`),
    KEY `idx_vector_id` (`vector_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG版本文档表（文档内容快照）';

-- 向量化表也应该是一个快照版本

-- ============================================
-- 7. 用户RAG表（用户安装的RAG）
-- ============================================
CREATE TABLE `user_rags` (
    `id` VARCHAR(36) NOT NULL COMMENT '安装记录ID，UUID',
    `user_id` VARCHAR(36) NOT NULL COMMENT '用户ID',
    `rag_version_id` VARCHAR(36) NOT NULL COMMENT '关联的RAG版本快照ID',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '安装时的名称',
    `description` TEXT COMMENT '安装时的描述',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '安装时的图标',
    `version` VARCHAR(50) DEFAULT NULL COMMENT '版本号',
    `installed_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '安装时间',
    `original_rag_id` VARCHAR(36) NOT NULL COMMENT '原始RAG数据集ID',
    `install_type` VARCHAR(20) DEFAULT 'SNAPSHOT' COMMENT '安装类型：REFERENCE/SNAPSHOT',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` TIMESTAMP NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_rag_version_id` (`rag_version_id`),
    KEY `idx_original_rag_id` (`original_rag_id`),
    KEY `idx_install_type` (`install_type`),
    KEY `idx_installed_at` (`installed_at`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户RAG表（用户安装的RAG）';

-- ============================================
-- 8. 用户RAG文件表（用户RAG文件快照）
-- ============================================
CREATE TABLE `user_rag_files` (
    `id` VARCHAR(36) NOT NULL COMMENT '文件快照ID，UUID',
    `user_rag_id` VARCHAR(36) NOT NULL COMMENT '关联的用户RAG ID',
    `original_file_id` VARCHAR(36) DEFAULT NULL COMMENT '原始文件ID（仅标识）',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    `file_page_size` INTEGER DEFAULT NULL COMMENT '文件页数',
    `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件类型',
    `file_path` VARCHAR(500) DEFAULT NULL COMMENT '文件存储路径',
    `process_status` INTEGER DEFAULT NULL COMMENT '处理状态',
    `embedding_status` INTEGER DEFAULT NULL COMMENT '向量化状态',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` TIMESTAMP NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_rag_id` (`user_rag_id`),
    KEY `idx_original_file_id` (`original_file_id`),
    KEY `idx_file_name` (`file_name`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户RAG文件表（用户RAG文件快照）';

-- ============================================
-- 9. 用户RAG文档表（用户RAG文档快照）
-- ============================================
CREATE TABLE `user_rag_documents` (
    `id` VARCHAR(36) NOT NULL COMMENT '文档快照ID，UUID',
    `user_rag_id` VARCHAR(36) NOT NULL COMMENT '关联的用户RAG ID',
    `user_rag_file_id` VARCHAR(36) DEFAULT NULL COMMENT '关联的用户RAG文件ID',
    `original_document_id` VARCHAR(36) DEFAULT NULL COMMENT '原始文档单元ID（仅标识）',
    `content` TEXT COMMENT '文档内容',
    `page` INTEGER DEFAULT NULL COMMENT '页码',
    `vector_id` VARCHAR(100) DEFAULT NULL COMMENT '向量ID（在向量数据库中的ID）',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` TIMESTAMP NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_rag_id` (`user_rag_id`),
    KEY `idx_user_rag_file_id` (`user_rag_file_id`),
    KEY `idx_original_document_id` (`original_document_id`),
    KEY `idx_page` (`page`),
    KEY `idx_vector_id` (`vector_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户RAG文档表（用户RAG文档快照）';

-- ============================================
-- 10. 文档分片表（用于向量存储）
-- ============================================
create table public.vector_store (
                                     embedding_id uuid primary key not null,
                                     embedding vector(1024),
                                     text text,
                                     metadata json
);



-- ============================================
-- 表关系说明和索引优化
-- ============================================

-- 添加外键约束（可选，根据实际需要启用）
-- ALTER TABLE file_detail ADD CONSTRAINT fk_file_detail_dataset FOREIGN KEY (data_set_id) REFERENCES ai_rag_qa_dataset(id);
-- ALTER TABLE document_unit ADD CONSTRAINT fk_document_unit_file FOREIGN KEY (file_id) REFERENCES file_detail(id);
-- ALTER TABLE rag_version_files ADD CONSTRAINT fk_rag_version_files FOREIGN KEY (rag_version_id) REFERENCES rag_versions(id);
-- ALTER TABLE rag_version_documents ADD CONSTRAINT fk_rag_version_documents FOREIGN KEY (rag_version_id) REFERENCES rag_versions(id);
-- ALTER TABLE user_rags ADD CONSTRAINT fk_user_rags_version FOREIGN KEY (rag_version_id) REFERENCES rag_versions(id);
-- ALTER TABLE user_rag_files ADD CONSTRAINT fk_user_rag_files FOREIGN KEY (user_rag_id) REFERENCES user_rags(id);
-- ALTER TABLE user_rag_documents ADD CONSTRAINT fk_user_rag_documents FOREIGN KEY (user_rag_id) REFERENCES user_rags(id);

-- 复合索引优化（根据业务查询模式添加）
CREATE INDEX idx_file_detail_user_dataset ON file_detail(user_id, data_set_id);
CREATE INDEX idx_document_unit_file_vector ON document_unit(file_id, is_vector);
CREATE INDEX idx_document_unit_file_ocr ON document_unit(file_id, is_ocr);
CREATE INDEX idx_rag_versions_original_version ON rag_versions(original_rag_id, version);
CREATE INDEX idx_user_rags_user_original ON user_rags(user_id, original_rag_id);

-- ============================================
-- 数据字典说明
-- ============================================

/*
文件处理状态枚举 (processing_status):
0 - UPLOADED (已上传)
1 - OCR_PROCESSING (OCR处理中)
2 - OCR_COMPLETED (OCR处理完成)
3 - EMBEDDING_PROCESSING (向量化处理中)
4 - COMPLETED (处理完成)
5 - OCR_FAILED (OCR处理失败)
6 - EMBEDDING_FAILED (向量化处理失败)

RAG发布状态枚举 (publish_status):
1 - REVIEWING (审核中)
2 - PUBLISHED (已发布)
3 - REJECTED (拒绝)
4 - REMOVED (已下架)

安装类型枚举 (install_type):
'REFERENCE' - 引用类型（动态引用原始数据集，支持实时更新）
'SNAPSHOT' - 快照类型（使用版本快照数据，内容固定不变）

布尔值字段:
is_vector: TRUE表示已向量化，FALSE表示未向量化
is_ocr: TRUE表示OCR已完成，FALSE表示OCR未完成或失败
*/

-- 重置外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 创建完成提示
-- ============================================
-- SELECT 'RAG相关数据表创建完成！' as message;