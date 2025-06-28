-- 为工具表添加全局状态字段
ALTER TABLE tools ADD COLUMN is_global BOOLEAN NOT NULL DEFAULT false;

-- 添加字段注释
COMMENT ON COLUMN tools.is_global IS '是否为全局工具（true=全局工具，在系统级别部署；false=用户工具，需要在用户容器中部署）';

-- 为user_tools表也添加全局状态字段，用于跟踪用户安装的工具类型
ALTER TABLE user_tools ADD COLUMN is_global BOOLEAN NOT NULL DEFAULT false;

-- 添加字段注释
COMMENT ON COLUMN user_tools.is_global IS '是否为全局工具（继承自原始工具的全局状态）';
