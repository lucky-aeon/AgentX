-- 检查当前config_data的格式
SELECT 
    id,
    feature_key,
    config_data,
    pg_typeof(config_data) as data_type,
    config_data::text as raw_text
FROM auth_settings 
WHERE feature_key IN ('GITHUB_LOGIN', 'COMMUNITY_LOGIN') 
  AND config_data IS NOT NULL;

-- 如果config_data存储的是JSON字符串而不是JSONB对象，需要修复
-- 这个脚本会将 "{"key": "value"}" 转换为 {"key": "value"}

-- 检查是否需要修复（如果config_data以引号开头，说明是字符串）
SELECT 
    id,
    feature_key,
    config_data::text,
    CASE 
        WHEN config_data::text LIKE '"%"' THEN 'NEEDS_FIX'
        ELSE 'OK'
    END as status
FROM auth_settings 
WHERE feature_key IN ('GITHUB_LOGIN', 'COMMUNITY_LOGIN') 
  AND config_data IS NOT NULL;

-- 修复脚本（如果需要的话）
-- UPDATE auth_settings 
-- SET config_data = (config_data::text)::jsonb
-- WHERE feature_key IN ('GITHUB_LOGIN', 'COMMUNITY_LOGIN') 
--   AND config_data IS NOT NULL
--   AND config_data::text LIKE '"%"';