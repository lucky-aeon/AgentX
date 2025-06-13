-- AgentX 默认数据初始化脚本
-- 该脚本用于插入系统默认数据，包括默认管理员账号

-- 插入默认管理员用户
-- 密码: admin123 (经过BCrypt加密)
-- 注意：这里使用固定的加密密码，实际项目中应该使用应用程序的加密方法
INSERT INTO users (
    id, 
    nickname, 
    email, 
    phone, 
    password,
    created_at,
    updated_at
) VALUES (
    'admin-user-uuid-001', 
    'AgentX管理员', 
    'admin@agentx.ai', 
    '', 
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKXgwOh36JN9Z8qkCFgOyyCyVqSa',  -- BCrypt加密的"admin123"
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- 插入默认测试用户
-- 密码: test123 (经过BCrypt加密)
INSERT INTO users (
    id, 
    nickname, 
    email, 
    phone, 
    password,
    created_at,
    updated_at
) VALUES (
    'test-user-uuid-001', 
    '测试用户', 
    'test@agentx.ai', 
    '', 
    '$2a$10$6X8mJkr3nJj4dIm7sBXwKOrVNYVXq8P5J1QTgHb8LN9xF4P2QNzXO',  -- BCrypt加密的"test123"
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- 可以在这里添加其他默认数据
-- 比如默认的Agent、Provider、Model等

-- 插入完成提示
SELECT 'AgentX默认数据初始化完成' as message; 