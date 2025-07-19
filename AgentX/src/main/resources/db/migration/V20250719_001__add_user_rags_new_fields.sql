-- 为user_rags表添加新字段以支持动态引用和版本管理
-- Migration: V20250719_001__add_user_rags_new_fields.sql
-- Description: 添加原始RAG ID、安装类型字段，实现快照机制

-- 添加原始RAG数据集ID字段
ALTER TABLE user_rags ADD COLUMN original_rag_id VARCHAR(64);
COMMENT ON COLUMN user_rags.original_rag_id IS '原始RAG数据集ID';

-- 添加安装类型字段
ALTER TABLE user_rags ADD COLUMN install_type VARCHAR(20) DEFAULT 'SNAPSHOT';
COMMENT ON COLUMN user_rags.install_type IS '安装类型：REFERENCE(引用)/SNAPSHOT(快照)';

-- 更新现有数据：为兼容性设置默认值
UPDATE user_rags ur 
SET 
    original_rag_id = (
        SELECT rv.original_rag_id 
        FROM rag_versions rv 
        WHERE rv.id = ur.rag_version_id
    ),
    install_type = 'SNAPSHOT'
WHERE ur.original_rag_id IS NULL;

-- 为创建者的RAG设置为REFERENCE类型
UPDATE user_rags ur
SET install_type = 'REFERENCE'
WHERE EXISTS (
    SELECT 1 
    FROM rag_versions rv 
    WHERE rv.id = ur.rag_version_id 
    AND rv.user_id = ur.user_id
);

