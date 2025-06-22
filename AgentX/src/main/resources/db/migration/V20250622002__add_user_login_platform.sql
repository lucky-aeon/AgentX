-- 添加用户登录平台字段
ALTER TABLE users ADD COLUMN login_platform VARCHAR(50);

-- 为现有用户设置默认登录平台
-- 如果有GitHub ID，设置为github
UPDATE users SET login_platform = 'github' WHERE github_id IS NOT NULL;

-- 其他用户设置为普通登录
UPDATE users SET login_platform = 'normal' WHERE login_platform IS NULL;