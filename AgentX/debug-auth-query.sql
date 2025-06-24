-- 查询认证配置表数据的原始格式
SELECT 
    id, 
    feature_key, 
    config_data,
    config_data::text as config_data_text,
    pg_typeof(config_data) as data_type
FROM auth_settings 
WHERE feature_key IN ('GITHUB_LOGIN', 'COMMUNITY_LOGIN') 
ORDER BY feature_key;