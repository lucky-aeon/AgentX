
CREATE TABLE agent.file_detail (
                                     id varchar(64) NOT NULL, -- 文件id
                                     url text NULL, -- 文件访问地址
                                     "size" int8 NULL, -- 文件大小，单位字节
                                     filename varchar(255) NULL, -- 文件名称
                                     original_filename varchar(255) NULL, -- 原始文件名
                                     base_path varchar(255) NULL, -- 基础存储路径
                                     "path" varchar(255) NULL, -- 存储路径
                                     ext varchar(50) NULL, -- 文件扩展名
                                     content_type varchar(100) NULL, -- MIME类型
                                     platform varchar(50) NULL, -- 存储平台
                                     th_url text NULL, -- 缩略图访问路径
                                     th_filename varchar(255) NULL, -- 缩略图名称
                                     th_size int8 NULL, -- 缩略图大小，单位字节
                                     th_content_type varchar(100) NULL, -- 缩略图MIME类型
                                     object_id varchar(64) NULL, -- 文件所属对象id
                                     object_type varchar(50) NULL, -- 文件所属对象类型
                                     metadata text NULL, -- 文件元数据
                                     user_metadata text NULL, -- 文件用户元数据
                                     th_metadata text NULL, -- 缩略图元数据
                                     th_user_metadata text NULL, -- 缩略图用户元数据
                                     attr text NULL, -- 附加属性
                                     file_acl varchar(50) NULL, -- 文件ACL
                                     th_file_acl varchar(50) NULL, -- 缩略图文件ACL
                                     hash_info text NULL, -- 哈希信息
                                     upload_id varchar(64) NULL, -- 上传ID
                                     upload_status int4 NULL, -- 上传状态，1：初始化完成，2：上传完成
                                     user_id int8 NULL, -- 用户ID
                                     is_initialize varchar(20) NULL, -- 是否进行初始化
                                     is_embedding varchar(20) NULL, -- 是否进行向量化
                                     data_set_id varchar NULL,
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     deleted_at TIMESTAMP null,
                                     file_page_size int8 NULL, -- 文件页数
                                     CONSTRAINT file_detail_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE agent.file_detail IS '文件详情表';

-- Column comments

COMMENT ON COLUMN agent.file_detail.id IS '文件id';
COMMENT ON COLUMN agent.file_detail.url IS '文件访问地址';
COMMENT ON COLUMN agent.file_detail."size" IS '文件大小，单位字节';
COMMENT ON COLUMN agent.file_detail.filename IS '文件名称';
COMMENT ON COLUMN agent.file_detail.original_filename IS '原始文件名';
COMMENT ON COLUMN agent.file_detail.base_path IS '基础存储路径';
COMMENT ON COLUMN agent.file_detail."path" IS '存储路径';
COMMENT ON COLUMN agent.file_detail.ext IS '文件扩展名';
COMMENT ON COLUMN agent.file_detail.content_type IS 'MIME类型';
COMMENT ON COLUMN agent.file_detail.platform IS '存储平台';
COMMENT ON COLUMN agent.file_detail.th_url IS '缩略图访问路径';
COMMENT ON COLUMN agent.file_detail.th_filename IS '缩略图名称';
COMMENT ON COLUMN agent.file_detail.th_size IS '缩略图大小，单位字节';
COMMENT ON COLUMN agent.file_detail.th_content_type IS '缩略图MIME类型';
COMMENT ON COLUMN agent.file_detail.object_id IS '文件所属对象id';
COMMENT ON COLUMN agent.file_detail.object_type IS '文件所属对象类型';
COMMENT ON COLUMN agent.file_detail.metadata IS '文件元数据';
COMMENT ON COLUMN agent.file_detail.user_metadata IS '文件用户元数据';
COMMENT ON COLUMN agent.file_detail.th_metadata IS '缩略图元数据';
COMMENT ON COLUMN agent.file_detail.th_user_metadata IS '缩略图用户元数据';
COMMENT ON COLUMN agent.file_detail.attr IS '附加属性';
COMMENT ON COLUMN agent.file_detail.file_acl IS '文件ACL';
COMMENT ON COLUMN agent.file_detail.th_file_acl IS '缩略图文件ACL';
COMMENT ON COLUMN agent.file_detail.hash_info IS '哈希信息';
COMMENT ON COLUMN agent.file_detail.upload_id IS '上传ID';
COMMENT ON COLUMN agent.file_detail.upload_status IS '上传状态，1：初始化完成，2：上传完成';
COMMENT ON COLUMN agent.file_detail.user_id IS '用户ID';
COMMENT ON COLUMN agent.file_detail.is_initialize IS '是否进行初始化';
COMMENT ON COLUMN agent.file_detail.is_embedding IS '是否进行向量化';
COMMENT ON COLUMN agent.file_detail.file_page_size IS '文件页数';