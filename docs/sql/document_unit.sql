CREATE TABLE agent.document_unit (
                                     id varchar(64) NOT NULL, -- 文件id
                                     file_id varchar(64) NULL, -- 文档ID
                                     page int4 NULL, -- 页码
                                     "content" text NULL, -- 当前页内容
                                     flag int4 NULL, -- 标记
                                     is_vector bool NOT NULL, -- 是否进行了向量化
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     deleted_at TIMESTAMP null,
                                     CONSTRAINT document_unit_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE agent.document_unit IS '文档单元表';

-- Column comments

COMMENT ON COLUMN agent.document_unit.id IS '文件id';
COMMENT ON COLUMN agent.document_unit.file_id IS '文档ID';
COMMENT ON COLUMN agent.document_unit.page IS '页码';
COMMENT ON COLUMN agent.document_unit."content" IS '当前页内容';
COMMENT ON COLUMN agent.document_unit.flag IS '标记';
COMMENT ON COLUMN agent.document_unit.is_vector IS '是否进行了向量化';