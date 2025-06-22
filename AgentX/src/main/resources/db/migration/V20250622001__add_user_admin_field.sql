-- 为users表添加is_admin字段
-- is_admin用于标识用户是否为管理员，控制后台管理权限

ALTER TABLE users ADD COLUMN is_admin BOOLEAN DEFAULT FALSE COMMENT '是否为管理员';

-- 为现有的admin@agentx.ai用户设置管理员权限
UPDATE users SET is_admin = TRUE WHERE email = 'admin@agentx.ai';