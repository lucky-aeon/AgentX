-- 为models表添加model_endpoint字段
-- model_endpoint用于存储模型的部署名称，支持高可用场景下不同实例的不同部署名称

ALTER TABLE models ADD COLUMN model_endpoint VARCHAR(255) COMMENT '模型部署名称'; 