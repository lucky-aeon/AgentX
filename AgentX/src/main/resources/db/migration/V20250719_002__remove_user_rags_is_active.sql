-- 移除user_rags表的is_active字段
-- Migration: V20250719_002__remove_user_rags_is_active.sql
-- Description: 移除is_active字段，因为Agent使用插拔式设计，不需要全局启用/禁用

-- 删除is_active字段
ALTER TABLE user_rags DROP COLUMN is_active;