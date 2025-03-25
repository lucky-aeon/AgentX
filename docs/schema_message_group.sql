-- AgentX消息分组功能SQL脚本
-- 创建于：2025-03-19

-- =============================================
-- 消息分组表结构
-- =============================================

-- 创建消息组表（定义消息组）
CREATE TABLE message_groups (
    id VARCHAR(36) PRIMARY KEY,           -- 消息组唯一ID
    name VARCHAR(255) NOT NULL,           -- 消息组名称
    description TEXT,                     -- 消息组描述
    session_id VARCHAR(36) NOT NULL,      -- 所属会话ID
    created_at TIMESTAMP NOT NULL,        -- 创建时间
    updated_at TIMESTAMP NOT NULL,        -- 更新时间
    is_active BOOLEAN DEFAULT TRUE,       -- 是否活跃
    user_id VARCHAR(36) NOT NULL,         -- 创建人ID
    metadata JSON                        -- 元数据
);

-- 为message_groups表添加注释
COMMENT ON TABLE message_groups IS '消息组表，用于定义消息分组';
COMMENT ON COLUMN message_groups.id IS '消息组唯一ID';
COMMENT ON COLUMN message_groups.name IS '消息组名称';
COMMENT ON COLUMN message_groups.description IS '消息组描述';
COMMENT ON COLUMN message_groups.session_id IS '所属会话ID';
COMMENT ON COLUMN message_groups.created_at IS '创建时间';
COMMENT ON COLUMN message_groups.updated_at IS '更新时间';
COMMENT ON COLUMN message_groups.is_active IS '是否活跃';
COMMENT ON COLUMN message_groups.user_id IS '创建人ID';
COMMENT ON COLUMN message_groups.metadata IS '元数据';

-- 创建消息-组关联表（将消息关联到组）
CREATE TABLE message_group_items (
    id VARCHAR(36) PRIMARY KEY,           -- 关联唯一ID
    group_id VARCHAR(36) NOT NULL,        -- 消息组ID
    message_id VARCHAR(36) NOT NULL,      -- 消息ID
    created_at TIMESTAMP NOT NULL,        -- 创建时间
    sort_order INTEGER DEFAULT 0,         -- 排序顺序
    metadata JSON,                        -- 元数据
);

-- 为message_group_items表添加注释
COMMENT ON TABLE message_group_items IS '消息-组关联表，将消息关联到组';
COMMENT ON COLUMN message_group_items.id IS '关联唯一ID';
COMMENT ON COLUMN message_group_items.group_id IS '消息组ID';
COMMENT ON COLUMN message_group_items.message_id IS '消息ID';
COMMENT ON COLUMN message_group_items.created_at IS '创建时间';
COMMENT ON COLUMN message_group_items.sort_order IS '排序顺序';
COMMENT ON COLUMN message_group_items.metadata IS '元数据';

-- 创建组标签表（为消息组添加标签）
CREATE TABLE message_group_tags (
    id VARCHAR(36) PRIMARY KEY,           -- 标签唯一ID
    group_id VARCHAR(36) NOT NULL,        -- 消息组ID
    tag_name VARCHAR(100) NOT NULL,       -- 标签名称
    created_at TIMESTAMP NOT NULL,        -- 创建时间
);

-- 为message_group_tags表添加注释
COMMENT ON TABLE message_group_tags IS '组标签表，为消息组添加标签';
COMMENT ON COLUMN message_group_tags.id IS '标签唯一ID';
COMMENT ON COLUMN message_group_tags.group_id IS '消息组ID';
COMMENT ON COLUMN message_group_tags.tag_name IS '标签名称';
COMMENT ON COLUMN message_group_tags.created_at IS '创建时间';

-- 创建话题关联表（用于构建话题树结构）
CREATE TABLE topic_relations (
    id VARCHAR(36) PRIMARY KEY,           -- 关联唯一ID
    parent_id VARCHAR(36) NOT NULL,       -- 父话题ID
    child_id VARCHAR(36) NOT NULL,        -- 子话题ID
    relation_type VARCHAR(50),            -- 关联类型
    created_at TIMESTAMP NOT NULL,        -- 创建时间
    metadata JSON                        -- 关联元数据
);

-- 为topic_relations表添加注释
COMMENT ON TABLE topic_relations IS '话题关联表，用于构建话题树结构';
COMMENT ON COLUMN topic_relations.id IS '关联唯一ID';
COMMENT ON COLUMN topic_relations.parent_id IS '父话题ID';
COMMENT ON COLUMN topic_relations.child_id IS '子话题ID';
COMMENT ON COLUMN topic_relations.relation_type IS '关联类型';
COMMENT ON COLUMN topic_relations.created_at IS '创建时间';
COMMENT ON COLUMN topic_relations.metadata IS '关联元数据';

-- =============================================
-- 索引定义
-- =============================================

-- 消息组索引
CREATE INDEX idx_message_groups_session_id ON message_groups(session_id);
CREATE INDEX idx_message_groups_user_id ON message_groups(user_id);
CREATE INDEX idx_message_groups_created_at ON message_groups(created_at);

-- 消息-组关联索引
CREATE INDEX idx_message_group_items_group_id ON message_group_items(group_id);
CREATE INDEX idx_message_group_items_message_id ON message_group_items(message_id);

-- 组标签索引
CREATE INDEX idx_message_group_tags_group_id ON message_group_tags(group_id);
CREATE INDEX idx_message_group_tags_tag_name ON message_group_tags(tag_name);

-- 创建话题关联索引
CREATE INDEX idx_topic_relations_parent ON topic_relations(parent_id);
CREATE INDEX idx_topic_relations_child ON topic_relations(child_id); 