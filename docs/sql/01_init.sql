-- 创建 pgvector 扩展（向量数据库支持）
CREATE EXTENSION IF NOT EXISTS vector;

create table public.accounts (
                                 id character varying(64) primary key not null,
                                 user_id character varying(64) not null, -- 用户ID
                                 balance numeric(20,8) default 0.00000000, -- 账户余额
                                 credit numeric(20,8) default 0.00000000, -- 信用额度
                                 total_consumed numeric(20,8) default 0.00000000, -- 总消费金额
                                 last_transaction_at timestamp without time zone, -- 最后交易时间
                                 deleted_at timestamp without time zone,
                                 created_at timestamp without time zone default CURRENT_TIMESTAMP,
                                 updated_at timestamp without time zone default CURRENT_TIMESTAMP
);
comment on table public.accounts is '用户账户表，存储用户余额和消费记录';
comment on column public.accounts.user_id is '用户ID';
comment on column public.accounts.balance is '账户余额';
comment on column public.accounts.credit is '信用额度';
comment on column public.accounts.total_consumed is '总消费金额';
comment on column public.accounts.last_transaction_at is '最后交易时间';

create table public.agent_execution_details (
                                                id bigserial primary key not null,
                                                trace_id character varying(64) not null, -- 关联汇总表的追踪ID
                                                sequence_no integer not null, -- 执行序号，同一trace_id内递增
                                                step_type character varying(32) not null, -- 步骤类型：USER_MESSAGE, AI_RESPONSE, TOOL_CALL
                                                message_content text, -- 统一的消息内容（用户消息/AI响应/工具调用描述）
                                                message_type character varying(32), -- 消息类型：USER_MESSAGE, AI_RESPONSE, TOOL_CALL
                                                model_id character varying(128), -- 此次使用的模型ID
                                                provider_name character varying(64),
                                                message_tokens integer,
                                                model_call_time integer,
                                                tool_name character varying(128),
                                                tool_request_args text, -- 工具调用入参(JSON格式)
                                                tool_response_data text, -- 工具调用出参(JSON格式)
                                                tool_execution_time integer,
                                                tool_success boolean,
                                                is_fallback_used boolean default false, -- 是否触发了平替/降级
                                                fallback_reason text,
                                                fallback_from_model character varying(128),
                                                fallback_to_model character varying(128),
                                                step_cost numeric(10,6),
                                                step_success boolean not null,
                                                step_error_message text,
                                                created_at timestamp without time zone default CURRENT_TIMESTAMP,
                                                updated_at timestamp without time zone default CURRENT_TIMESTAMP,
                                                deleted_at timestamp without time zone
);
create index idx_agent_exec_details_trace_seq on agent_execution_details using btree (trace_id, sequence_no);
create index idx_agent_exec_details_trace_type on agent_execution_details using btree (trace_id, step_type);
create index idx_agent_exec_details_tool on agent_execution_details using btree (tool_name);
create index idx_agent_exec_details_model on agent_execution_details using btree (model_id);
comment on table public.agent_execution_details is 'Agent执行链路详细记录表，记录每次执行的详细过程';
comment on column public.agent_execution_details.trace_id is '关联汇总表的追踪ID';
comment on column public.agent_execution_details.sequence_no is '执行序号，同一trace_id内递增';
comment on column public.agent_execution_details.step_type is '步骤类型：USER_MESSAGE, AI_RESPONSE, TOOL_CALL';
comment on column public.agent_execution_details.message_content is '统一的消息内容（用户消息/AI响应/工具调用描述）';
comment on column public.agent_execution_details.message_type is '消息类型：USER_MESSAGE, AI_RESPONSE, TOOL_CALL';
comment on column public.agent_execution_details.model_id is '此次使用的模型ID';
comment on column public.agent_execution_details.tool_request_args is '工具调用入参(JSON格式)';
comment on column public.agent_execution_details.tool_response_data is '工具调用出参(JSON格式)';
comment on column public.agent_execution_details.is_fallback_used is '是否触发了平替/降级';

create table public.agent_execution_summary (
                                                id bigserial primary key not null,
                                                trace_id character varying(64) not null, -- 执行追踪ID，唯一标识一次完整执行
                                                user_id character varying(64) not null, -- 用户ID (String类型UUID)
                                                session_id character varying(64) not null, -- 会话ID
                                                agent_id character varying(64) not null, -- Agent ID (String类型UUID)
                                                execution_start_time timestamp without time zone not null,
                                                execution_end_time timestamp without time zone,
                                                total_execution_time integer, -- 总执行时间(毫秒)
                                                total_input_tokens integer default 0,
                                                total_output_tokens integer default 0,
                                                total_tokens integer default 0, -- 总Token数
                                                tool_call_count integer default 0, -- 工具调用总次数
                                                total_tool_execution_time integer default 0,
                                                total_cost numeric(10,6) default 0, -- 总成本费用
                                                execution_success boolean not null, -- 执行是否成功
                                                error_phase character varying(64),
                                                error_message text,
                                                created_at timestamp without time zone default CURRENT_TIMESTAMP,
                                                updated_at timestamp without time zone default CURRENT_TIMESTAMP,
                                                deleted_at timestamp without time zone
);
create unique index agent_execution_summary_trace_id_key on agent_execution_summary using btree (trace_id);
create index idx_agent_exec_summary_user_time on agent_execution_summary using btree (user_id, execution_start_time);
create index idx_agent_exec_summary_session on agent_execution_summary using btree (session_id);
create index idx_agent_exec_summary_agent on agent_execution_summary using btree (agent_id);
create index idx_agent_exec_summary_trace on agent_execution_summary using btree (trace_id);
comment on table public.agent_execution_summary is 'Agent执行链路汇总表，记录每次Agent执行的汇总信息';
comment on column public.agent_execution_summary.trace_id is '执行追踪ID，唯一标识一次完整执行';
comment on column public.agent_execution_summary.user_id is '用户ID (String类型UUID)';
comment on column public.agent_execution_summary.session_id is '会话ID';
comment on column public.agent_execution_summary.agent_id is 'Agent ID (String类型UUID)';
comment on column public.agent_execution_summary.total_execution_time is '总执行时间(毫秒)';
comment on column public.agent_execution_summary.total_tokens is '总Token数';
comment on column public.agent_execution_summary.tool_call_count is '工具调用总次数';
comment on column public.agent_execution_summary.total_cost is '总成本费用';
comment on column public.agent_execution_summary.execution_success is '执行是否成功';

create table public.agent_tasks (
                                    id character varying(36) primary key not null, -- 任务ID
                                    session_id character varying(36) not null, -- 所属会话ID
                                    user_id character varying(36) not null, -- 用户ID
                                    parent_task_id character varying(36), -- 父任务ID
                                    task_name character varying(255) not null, -- 任务名称
                                    description text, -- 任务描述
                                    status character varying(20), -- 任务状态
                                    progress integer default 0, -- 任务进度,存放父任务中
                                    start_time timestamp without time zone, -- 开始时间
                                    end_time timestamp without time zone, -- 结束时间
                                    task_result text, -- 任务结果
                                    created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                                    updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                                    deleted_at timestamp without time zone -- 逻辑删除时间
);
create index idx_agent_tasks_session_id on agent_tasks using btree (session_id);
create index idx_agent_tasks_user_id on agent_tasks using btree (user_id);
create index idx_agent_tasks_parent_task_id on agent_tasks using btree (parent_task_id);
comment on table public.agent_tasks is '任务实体类';
comment on column public.agent_tasks.id is '任务ID';
comment on column public.agent_tasks.session_id is '所属会话ID';
comment on column public.agent_tasks.user_id is '用户ID';
comment on column public.agent_tasks.parent_task_id is '父任务ID';
comment on column public.agent_tasks.task_name is '任务名称';
comment on column public.agent_tasks.description is '任务描述';
comment on column public.agent_tasks.status is '任务状态';
comment on column public.agent_tasks.progress is '任务进度,存放父任务中';
comment on column public.agent_tasks.start_time is '开始时间';
comment on column public.agent_tasks.end_time is '结束时间';
comment on column public.agent_tasks.task_result is '任务结果';
comment on column public.agent_tasks.created_at is '创建时间';
comment on column public.agent_tasks.updated_at is '更新时间';
comment on column public.agent_tasks.deleted_at is '逻辑删除时间';

create table public.agent_versions (
                                       id character varying(36) primary key not null, -- 版本唯一ID
                                       agent_id character varying(36) not null, -- 关联的Agent ID
                                       name character varying(255) not null, -- Agent名称
                                       avatar character varying(255), -- Agent头像URL
                                       description text, -- Agent描述
                                       version_number character varying(20) not null, -- 版本号，如1.0.0
                                       system_prompt text, -- Agent系统提示词
                                       welcome_message text, -- 欢迎消息
                                       tool_ids jsonb, -- Agent可使用的工具ID列表，JSON数组格式
                                       knowledge_base_ids jsonb, -- 关联的知识库ID列表，JSON数组格式
                                       change_log text, -- 版本更新日志
                                       publish_status integer default 1, -- 发布状态：1-审核中, 2-已发布, 3-拒绝, 4-已下架
                                       reject_reason text, -- 审核拒绝原因
                                       review_time timestamp without time zone, -- 审核时间
                                       published_at timestamp without time zone, -- 发布时间
                                       user_id character varying(36) not null, -- 创建者用户ID
                                       tool_preset_params jsonb,
                                       multi_modal boolean default false,
                                       created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                                       updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                                       deleted_at timestamp without time zone -- 逻辑删除时间
);
create index idx_agent_versions_agent_id on agent_versions using btree (agent_id);
create index idx_agent_versions_user_id on agent_versions using btree (user_id);
comment on table public.agent_versions is 'Agent版本实体类，代表一个Agent的发布版本';
comment on column public.agent_versions.id is '版本唯一ID';
comment on column public.agent_versions.agent_id is '关联的Agent ID';
comment on column public.agent_versions.name is 'Agent名称';
comment on column public.agent_versions.avatar is 'Agent头像URL';
comment on column public.agent_versions.description is 'Agent描述';
comment on column public.agent_versions.version_number is '版本号，如1.0.0';
comment on column public.agent_versions.system_prompt is 'Agent系统提示词';
comment on column public.agent_versions.welcome_message is '欢迎消息';
comment on column public.agent_versions.tool_ids is 'Agent可使用的工具ID列表，JSON数组格式';
comment on column public.agent_versions.knowledge_base_ids is '关联的知识库ID列表，JSON数组格式';
comment on column public.agent_versions.change_log is '版本更新日志';
comment on column public.agent_versions.publish_status is '发布状态：1-审核中, 2-已发布, 3-拒绝, 4-已下架';
comment on column public.agent_versions.reject_reason is '审核拒绝原因';
comment on column public.agent_versions.review_time is '审核时间';
comment on column public.agent_versions.published_at is '发布时间';
comment on column public.agent_versions.user_id is '创建者用户ID';
comment on column public.agent_versions.created_at is '创建时间';
comment on column public.agent_versions.updated_at is '更新时间';
comment on column public.agent_versions.deleted_at is '逻辑删除时间';

create table public.agent_workspace (
                                        id character varying(36) primary key not null, -- 主键ID
                                        agent_id character varying(36) not null, -- Agent ID
                                        user_id character varying(36) not null, -- 用户ID
                                        llm_model_config jsonb, -- 模型配置，JSON格式
                                        created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                                        updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                                        deleted_at timestamp without time zone -- 逻辑删除时间
);
create index idx_agent_workspace_agent_id on agent_workspace using btree (agent_id);
create index idx_agent_workspace_user_id on agent_workspace using btree (user_id);
comment on table public.agent_workspace is 'Agent工作区实体类，用于记录用户添加到工作区的Agent';
comment on column public.agent_workspace.id is '主键ID';
comment on column public.agent_workspace.agent_id is 'Agent ID';
comment on column public.agent_workspace.user_id is '用户ID';
comment on column public.agent_workspace.llm_model_config is '模型配置，JSON格式';
comment on column public.agent_workspace.created_at is '创建时间';
comment on column public.agent_workspace.updated_at is '更新时间';
comment on column public.agent_workspace.deleted_at is '逻辑删除时间';

create table public.agents (
                               id character varying(36) primary key not null, -- Agent唯一ID
                               name character varying(255) not null, -- Agent名称
                               avatar character varying(255), -- Agent头像URL
                               description text, -- Agent描述
                               system_prompt text, -- Agent系统提示词
                               welcome_message text, -- 欢迎消息
                               tool_ids jsonb,
                               published_version character varying(36), -- 当前发布的版本ID
                               enabled boolean default true, -- Agent状态：TRUE-启用，FALSE-禁用
                               user_id character varying(36) not null, -- 创建者用户ID
                               tool_preset_params jsonb,
                               multi_modal boolean default false,
                               created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                               updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                               deleted_at timestamp without time zone, -- 逻辑删除时间
                               knowledge_base_ids jsonb -- 关联的知识库ID列表，JSON数组格式，用于RAG功能
);
create index idx_agents_user_id on agents using btree (user_id);
comment on table public.agents is 'Agent实体类，代表一个AI助手';
comment on column public.agents.id is 'Agent唯一ID';
comment on column public.agents.name is 'Agent名称';
comment on column public.agents.avatar is 'Agent头像URL';
comment on column public.agents.description is 'Agent描述';
comment on column public.agents.system_prompt is 'Agent系统提示词';
comment on column public.agents.welcome_message is '欢迎消息';
comment on column public.agents.published_version is '当前发布的版本ID';
comment on column public.agents.enabled is 'Agent状态：TRUE-启用，FALSE-禁用';
comment on column public.agents.user_id is '创建者用户ID';
comment on column public.agents.created_at is '创建时间';
comment on column public.agents.updated_at is '更新时间';
comment on column public.agents.deleted_at is '逻辑删除时间';
comment on column public.agents.knowledge_base_ids is '关联的知识库ID列表，JSON数组格式，用于RAG功能';

create table public.ai_rag_qa_dataset (
                                          id character varying(64) primary key not null,
                                          name character varying(64),
                                          icon character varying(64),
                                          description character varying(64),
                                          user_id character varying(64),
                                          created_at timestamp without time zone not null default CURRENT_TIMESTAMP,
                                          updated_at timestamp without time zone not null default CURRENT_TIMESTAMP,
                                          deleted_at timestamp without time zone
);

create table public.api_keys (
                                 id character varying(36) primary key not null, -- API Key ID
                                 api_key character varying(64) not null, -- API密钥
                                 agent_id character varying(36) not null, -- 关联的Agent ID
                                 user_id character varying(36) not null, -- 创建者用户ID
                                 name character varying(100), -- API Key名称/描述
                                 status boolean default true, -- 状态：TRUE-启用，FALSE-禁用
                                 usage_count integer default 0, -- 已使用次数
                                 last_used_at timestamp without time zone, -- 最后使用时间
                                 expires_at timestamp without time zone, -- 过期时间
                                 created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                                 updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                                 deleted_at timestamp without time zone -- 逻辑删除时间
);
create unique index api_keys_api_key_key on api_keys using btree (api_key);
comment on table public.api_keys is 'API密钥管理表';
comment on column public.api_keys.id is 'API Key ID';
comment on column public.api_keys.api_key is 'API密钥';
comment on column public.api_keys.agent_id is '关联的Agent ID';
comment on column public.api_keys.user_id is '创建者用户ID';
comment on column public.api_keys.name is 'API Key名称/描述';
comment on column public.api_keys.status is '状态：TRUE-启用，FALSE-禁用';
comment on column public.api_keys.usage_count is '已使用次数';
comment on column public.api_keys.last_used_at is '最后使用时间';
comment on column public.api_keys.expires_at is '过期时间';
comment on column public.api_keys.created_at is '创建时间';
comment on column public.api_keys.updated_at is '更新时间';
comment on column public.api_keys.deleted_at is '逻辑删除时间';

create table public.auth_settings (
                                      id character varying(36) primary key not null, -- 配置记录唯一ID
                                      feature_type character varying(50) not null, -- 功能类型：LOGIN-登录功能，REGISTER-注册功能
                                      feature_key character varying(100) not null, -- 功能键：NORMAL_LOGIN, GITHUB_LOGIN, COMMUNITY_LOGIN, USER_REGISTER等
                                      feature_name character varying(100) not null, -- 功能显示名称
                                      enabled boolean default true, -- 是否启用该功能
                                      config_data jsonb, -- 功能配置数据，JSON格式，存储SSO配置等
                                      display_order integer default 0, -- 显示顺序
                                      description text, -- 功能描述
                                      created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                                      updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                                      deleted_at timestamp without time zone -- 逻辑删除时间
);
create unique index auth_settings_feature_key_key on auth_settings using btree (feature_key);
comment on table public.auth_settings is '认证配置表，管理登录方式和注册功能的开关';
comment on column public.auth_settings.id is '配置记录唯一ID';
comment on column public.auth_settings.feature_type is '功能类型：LOGIN-登录功能，REGISTER-注册功能';
comment on column public.auth_settings.feature_key is '功能键：NORMAL_LOGIN, GITHUB_LOGIN, COMMUNITY_LOGIN, USER_REGISTER等';
comment on column public.auth_settings.feature_name is '功能显示名称';
comment on column public.auth_settings.enabled is '是否启用该功能';
comment on column public.auth_settings.config_data is '功能配置数据，JSON格式，存储SSO配置等';
comment on column public.auth_settings.display_order is '显示顺序';
comment on column public.auth_settings.description is '功能描述';
comment on column public.auth_settings.created_at is '创建时间';
comment on column public.auth_settings.updated_at is '更新时间';
comment on column public.auth_settings.deleted_at is '逻辑删除时间';

create table public.container_templates (
                                            id character varying(36) primary key not null, -- 模板ID
                                            name character varying(100) not null, -- 模板名称
                                            description text, -- 模板描述
                                            type character varying(50) not null, -- 模板类型(mcp-gateway等)
                                            image character varying(200) not null, -- 容器镜像名称
                                            image_tag character varying(50), -- 镜像版本标签
                                            internal_port integer not null, -- 容器内部端口
                                            cpu_limit numeric(4,2) not null, -- CPU限制(核数)
                                            memory_limit integer not null, -- 内存限制(MB)
                                            environment text, -- 环境变量配置(JSON格式)
                                            volume_mount_path character varying(500), -- 数据卷挂载路径
                                            command text, -- 启动命令(JSON数组格式)
                                            network_mode character varying(50), -- 网络模式
                                            restart_policy character varying(50), -- 重启策略
                                            health_check text, -- 健康检查配置(JSON格式)
                                            resource_config text, -- 资源配置(JSON格式)
                                            enabled boolean not null default true, -- 是否启用
                                            is_default boolean not null default false, -- 是否为默认模板
                                            created_by character varying(36), -- 创建者用户ID
                                            sort_order integer not null default 0, -- 排序权重
                                            created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                                            updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                                            deleted_at timestamp without time zone -- 删除时间
);
create unique index container_templates_name_key on container_templates using btree (name);
create index idx_container_templates_type on container_templates using btree (type);
create index idx_container_templates_enabled on container_templates using btree (enabled);
create index idx_container_templates_is_default on container_templates using btree (is_default);
create index idx_container_templates_created_by on container_templates using btree (created_by);
create index idx_container_templates_sort_order on container_templates using btree (sort_order);
create index idx_container_templates_created_at on container_templates using btree (created_at);
create index idx_container_templates_type_enabled_default on container_templates using btree (type, enabled, is_default);
create unique index idx_container_templates_unique_default on container_templates using btree (type) WHERE (is_default = true);
comment on table public.container_templates is '容器模板表';
comment on column public.container_templates.id is '模板ID';
comment on column public.container_templates.name is '模板名称';
comment on column public.container_templates.description is '模板描述';
comment on column public.container_templates.type is '模板类型(mcp-gateway等)';
comment on column public.container_templates.image is '容器镜像名称';
comment on column public.container_templates.image_tag is '镜像版本标签';
comment on column public.container_templates.internal_port is '容器内部端口';
comment on column public.container_templates.cpu_limit is 'CPU限制(核数)';
comment on column public.container_templates.memory_limit is '内存限制(MB)';
comment on column public.container_templates.environment is '环境变量配置(JSON格式)';
comment on column public.container_templates.volume_mount_path is '数据卷挂载路径';
comment on column public.container_templates.command is '启动命令(JSON数组格式)';
comment on column public.container_templates.network_mode is '网络模式';
comment on column public.container_templates.restart_policy is '重启策略';
comment on column public.container_templates.health_check is '健康检查配置(JSON格式)';
comment on column public.container_templates.resource_config is '资源配置(JSON格式)';
comment on column public.container_templates.enabled is '是否启用';
comment on column public.container_templates.is_default is '是否为默认模板';
comment on column public.container_templates.created_by is '创建者用户ID';
comment on column public.container_templates.sort_order is '排序权重';
comment on column public.container_templates.created_at is '创建时间';
comment on column public.container_templates.updated_at is '更新时间';
comment on column public.container_templates.deleted_at is '删除时间';

create table public.context (
                                id character varying(36) primary key not null, -- 上下文唯一ID
                                session_id character varying(36) not null, -- 所属会话ID
                                active_messages jsonb, -- 活跃消息ID列表，JSON数组格式
                                summary text, -- 历史消息摘要
                                created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                                updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                                deleted_at timestamp without time zone -- 逻辑删除时间
);
create index idx_context_session_id on context using btree (session_id);
comment on table public.context is '上下文实体类，管理会话的上下文窗口';
comment on column public.context.id is '上下文唯一ID';
comment on column public.context.session_id is '所属会话ID';
comment on column public.context.active_messages is '活跃消息ID列表，JSON数组格式';
comment on column public.context.summary is '历史消息摘要';
comment on column public.context.created_at is '创建时间';
comment on column public.context.updated_at is '更新时间';
comment on column public.context.deleted_at is '逻辑删除时间';

create table public.document_unit (
                                      id character varying(64) primary key not null, -- 文件id
                                      file_id character varying(64), -- 文档ID
                                      page integer, -- 页码
                                      content text, -- 当前页内容
                                      flag integer, -- 标记
                                      is_vector boolean not null, -- 是否进行了向量化
                                      created_at timestamp without time zone not null default CURRENT_TIMESTAMP,
                                      updated_at timestamp without time zone not null default CURRENT_TIMESTAMP,
                                      deleted_at timestamp without time zone,
                                      is_ocr boolean
);
comment on table public.document_unit is '文档单元表';
comment on column public.document_unit.id is '文件id';
comment on column public.document_unit.file_id is '文档ID';
comment on column public.document_unit.page is '页码';
comment on column public.document_unit.content is '当前页内容';
comment on column public.document_unit.flag is '标记';
comment on column public.document_unit.is_vector is '是否进行了向量化';

create table public.file_detail (
                                    id character varying(64) primary key not null, -- 文件id
                                    url text, -- 文件访问地址
                                    size bigint, -- 文件大小，单位字节
                                    filename character varying(255), -- 文件名称
                                    original_filename character varying(255), -- 原始文件名
                                    base_path character varying(255), -- 基础存储路径
                                    path character varying(255), -- 存储路径
                                    ext character varying(50), -- 文件扩展名
                                    content_type character varying(100), -- MIME类型
                                    platform character varying(50), -- 存储平台
                                    th_url text, -- 缩略图访问路径
                                    th_filename character varying(255), -- 缩略图名称
                                    th_size bigint, -- 缩略图大小，单位字节
                                    th_content_type character varying(100), -- 缩略图MIME类型
                                    object_id character varying(64), -- 文件所属对象id
                                    object_type character varying(50), -- 文件所属对象类型
                                    metadata text, -- 文件元数据
                                    user_metadata text, -- 文件用户元数据
                                    th_metadata text, -- 缩略图元数据
                                    th_user_metadata text, -- 缩略图用户元数据
                                    attr text, -- 附加属性
                                    file_acl character varying(50), -- 文件ACL
                                    th_file_acl character varying(50), -- 缩略图文件ACL
                                    hash_info text, -- 哈希信息
                                    upload_id character varying(64), -- 上传ID
                                    upload_status integer, -- 上传状态，1：初始化完成，2：上传完成
                                    user_id character varying, -- 用户ID
                                    data_set_id character varying,
                                    created_at timestamp without time zone not null default CURRENT_TIMESTAMP,
                                    updated_at timestamp without time zone not null default CURRENT_TIMESTAMP,
                                    deleted_at timestamp without time zone,
                                    file_page_size bigint, -- 文件页数
                                    current_page_number integer default 0,
                                    process_progress numeric(5,2) default 0.00,
                                    current_ocr_page_number integer default 0, -- 当前OCR处理页数
                                    current_embedding_page_number integer default 0, -- 当前向量化处理页数
                                    ocr_process_progress numeric(5,2) default 0.00, -- OCR处理进度百分比
                                    embedding_process_progress numeric(5,2) default 0.00, -- 向量化处理进度百分比(0-100)
                                    processing_status integer default 0 -- 文件处理状态：0-已上传，1-OCR处理中，2-OCR完成，3-向量化处理中，4-处理完成，5-OCR失败，6-向量化失败
);
create index idx_file_detail_current_page on file_detail using btree (current_page_number);
create index idx_file_detail_progress on file_detail using btree (process_progress);
create index idx_file_detail_current_ocr_page on file_detail using btree (current_ocr_page_number);
create index idx_file_detail_current_embedding_page on file_detail using btree (current_embedding_page_number);
create index idx_file_detail_ocr_progress on file_detail using btree (ocr_process_progress);
create index idx_file_detail_embedding_progress on file_detail using btree (embedding_process_progress);
comment on table public.file_detail is '文件详情表';
comment on column public.file_detail.id is '文件id';
comment on column public.file_detail.url is '文件访问地址';
comment on column public.file_detail.size is '文件大小，单位字节';
comment on column public.file_detail.filename is '文件名称';
comment on column public.file_detail.original_filename is '原始文件名';
comment on column public.file_detail.base_path is '基础存储路径';
comment on column public.file_detail.path is '存储路径';
comment on column public.file_detail.ext is '文件扩展名';
comment on column public.file_detail.content_type is 'MIME类型';
comment on column public.file_detail.platform is '存储平台';
comment on column public.file_detail.th_url is '缩略图访问路径';
comment on column public.file_detail.th_filename is '缩略图名称';
comment on column public.file_detail.th_size is '缩略图大小，单位字节';
comment on column public.file_detail.th_content_type is '缩略图MIME类型';
comment on column public.file_detail.object_id is '文件所属对象id';
comment on column public.file_detail.object_type is '文件所属对象类型';
comment on column public.file_detail.metadata is '文件元数据';
comment on column public.file_detail.user_metadata is '文件用户元数据';
comment on column public.file_detail.th_metadata is '缩略图元数据';
comment on column public.file_detail.th_user_metadata is '缩略图用户元数据';
comment on column public.file_detail.attr is '附加属性';
comment on column public.file_detail.file_acl is '文件ACL';
comment on column public.file_detail.th_file_acl is '缩略图文件ACL';
comment on column public.file_detail.hash_info is '哈希信息';
comment on column public.file_detail.upload_id is '上传ID';
comment on column public.file_detail.upload_status is '上传状态，1：初始化完成，2：上传完成';
comment on column public.file_detail.user_id is '用户ID';
comment on column public.file_detail.file_page_size is '文件页数';
comment on column public.file_detail.current_ocr_page_number is '当前OCR处理页数';
comment on column public.file_detail.current_embedding_page_number is '当前向量化处理页数';
comment on column public.file_detail.ocr_process_progress is 'OCR处理进度百分比';
comment on column public.file_detail.embedding_process_progress is '向量化处理进度百分比(0-100)';
comment on column public.file_detail.processing_status is '文件处理状态：0-已上传，1-OCR处理中，2-OCR完成，3-向量化处理中，4-处理完成，5-OCR失败，6-向量化失败';

create table public.messages (
                                 id character varying(36) primary key not null, -- 消息唯一ID
                                 session_id character varying(36) not null, -- 所属会话ID
                                 role character varying(20) not null, -- 消息角色 (user, assistant, system)
                                 content text not null, -- 消息内容
                                 message_type character varying(20) not null default 'TEXT', -- 消息类型
                                 token_count integer default 0, -- Token数量
                                 body_token_count INTEGER DEFAULT 0, -- 消息本体的token数量
                                 provider character varying(50), -- 服务提供商
                                 model character varying(50), -- 使用的模型
                                 metadata jsonb, -- 消息元数据，JSON格式
                                 file_urls jsonb,
                                 created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                                 updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                                 deleted_at timestamp without time zone -- 逻辑删除时间
);
create index idx_messages_session_id on messages using btree (session_id);
comment on table public.messages is '消息实体类，代表对话中的一条消息';
comment on column public.messages.id is '消息唯一ID';
comment on column public.messages.session_id is '所属会话ID';
comment on column public.messages.role is '消息角色 (user, assistant, system)';
comment on column public.messages.content is '消息内容';
comment on column public.messages.message_type is '消息类型';
comment on column public.messages.token_count is 'Token数量';
comment on column public.messages.body_token_count is '消息本体的token数量';
comment on column public.messages.provider is '服务提供商';
comment on column public.messages.model is '使用的模型';
comment on column public.messages.metadata is '消息元数据，JSON格式';
comment on column public.messages.created_at is '创建时间';
comment on column public.messages.updated_at is '更新时间';
comment on column public.messages.deleted_at is '逻辑删除时间';

create table public.models (
                               id character varying(36) primary key not null, -- 模型ID
                               user_id character varying(36), -- 用户ID
                               provider_id character varying(36) not null, -- 服务提供商ID
                               model_id character varying(100) not null, -- 模型ID标识
                               name character varying(100) not null, -- 模型名称
                               model_endpoint character varying(255) not null,
                               description text, -- 模型描述
                               is_official boolean default false, -- 是否官方模型
                               type character varying(20) not null, -- 模型类型
                               status boolean default true, -- 模型状态
                               created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                               updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                               deleted_at timestamp without time zone -- 逻辑删除时间
);
create index idx_models_provider_id on models using btree (provider_id);
create index idx_models_user_id on models using btree (user_id);
comment on table public.models is '模型领域模型';
comment on column public.models.id is '模型ID';
comment on column public.models.user_id is '用户ID';
comment on column public.models.provider_id is '服务提供商ID';
comment on column public.models.model_id is '模型ID标识';
comment on column public.models.name is '模型名称';
comment on column public.models.description is '模型描述';
comment on column public.models.is_official is '是否官方模型';
comment on column public.models.type is '模型类型';
comment on column public.models.status is '模型状态';
comment on column public.models.created_at is '创建时间';
comment on column public.models.updated_at is '更新时间';
comment on column public.models.deleted_at is '逻辑删除时间';

create table public.orders (
                               id character varying(64) primary key not null, -- 订单唯一ID
                               user_id character varying(64) not null, -- 用户ID
                               order_no character varying(100) not null, -- 订单号（唯一）
                               order_type character varying(50) not null, -- 订单类型：RECHARGE(充值)、PURCHASE(购买)、SUBSCRIPTION(订阅)
                               title character varying(255) not null, -- 订单标题
                               description text, -- 订单描述
                               amount numeric(20,8) not null, -- 订单金额
                               currency character varying(10) default 'CNY', -- 货币代码，默认CNY
                               status integer not null default 1, -- 订单状态：1-待支付，2-已支付，3-已取消，4-已退款，5-已过期
                               expired_at timestamp without time zone, -- 订单过期时间
                               paid_at timestamp without time zone, -- 支付完成时间
                               cancelled_at timestamp without time zone, -- 取消时间
                               refunded_at timestamp without time zone, -- 退款时间
                               refund_amount numeric(20,8) default 0.00000000, -- 退款金额
                               payment_platform character varying(50), -- 支付平台：alipay(支付宝)、wechat(微信支付)、stripe(Stripe)
                               payment_type character varying(50), -- 支付类型：web(网页支付)、qr_code(二维码支付)、mobile(移动端支付)、h5(H5支付)、mini_program(小程序支付)
                               provider_order_id character varying(200), -- 第三方支付平台的订单ID，用于查询支付状态和对账
                               metadata jsonb, -- 订单扩展信息（JSONB格式）
                               deleted_at timestamp without time zone,
                               created_at timestamp without time zone default CURRENT_TIMESTAMP,
                               updated_at timestamp without time zone default CURRENT_TIMESTAMP
);
create unique index orders_order_no_key on orders using btree (order_no);
comment on table public.orders is '订单表，存储各种类型的订单信息和支付方式';
comment on column public.orders.id is '订单唯一ID';
comment on column public.orders.user_id is '用户ID';
comment on column public.orders.order_no is '订单号（唯一）';
comment on column public.orders.order_type is '订单类型：RECHARGE(充值)、PURCHASE(购买)、SUBSCRIPTION(订阅)';
comment on column public.orders.title is '订单标题';
comment on column public.orders.description is '订单描述';
comment on column public.orders.amount is '订单金额';
comment on column public.orders.currency is '货币代码，默认CNY';
comment on column public.orders.status is '订单状态：1-待支付，2-已支付，3-已取消，4-已退款，5-已过期';
comment on column public.orders.expired_at is '订单过期时间';
comment on column public.orders.paid_at is '支付完成时间';
comment on column public.orders.cancelled_at is '取消时间';
comment on column public.orders.refunded_at is '退款时间';
comment on column public.orders.refund_amount is '退款金额';
comment on column public.orders.payment_platform is '支付平台：alipay(支付宝)、wechat(微信支付)、stripe(Stripe)';
comment on column public.orders.payment_type is '支付类型：web(网页支付)、qr_code(二维码支付)、mobile(移动端支付)、h5(H5支付)、mini_program(小程序支付)';
comment on column public.orders.provider_order_id is '第三方支付平台的订单ID，用于查询支付状态和对账';
comment on column public.orders.metadata is '订单扩展信息（JSONB格式）';

create table public.products (
                                 id character varying(64) primary key not null,
                                 name character varying(255) not null, -- 商品名称
                                 type character varying(50) not null, -- 计费类型：MODEL_USAGE, AGENT_CREATION, API_CALLS等
                                 service_id character varying(100) not null, -- 业务服务标识
                                 rule_id character varying(64) not null, -- 关联的规则ID
                                 pricing_config jsonb, -- 价格配置（JSONB格式）
                                 status integer default 1, -- 状态：1-激活，0-禁用
                                 deleted_at timestamp without time zone,
                                 created_at timestamp without time zone default CURRENT_TIMESTAMP,
                                 updated_at timestamp without time zone default CURRENT_TIMESTAMP
);
comment on table public.products is '计费商品表，存储可计费的服务和产品信息';
comment on column public.products.name is '商品名称';
comment on column public.products.type is '计费类型：MODEL_USAGE, AGENT_CREATION, API_CALLS等';
comment on column public.products.service_id is '业务服务标识';
comment on column public.products.rule_id is '关联的规则ID';
comment on column public.products.pricing_config is '价格配置（JSONB格式）';
comment on column public.products.status is '状态：1-激活，0-禁用';

create table public.providers (
                                  id character varying(36) primary key not null, -- 服务提供商ID
                                  user_id character varying(36), -- 用户ID
                                  protocol character varying(50) not null, -- 协议类型
                                  name character varying(100) not null, -- 服务提供商名称
                                  description text, -- 服务提供商描述
                                  config text, -- 服务提供商配置,加密后的值
                                  is_official boolean default false, -- 是否官方服务提供商
                                  status boolean default true, -- 服务提供商状态
                                  created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                                  updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                                  deleted_at timestamp without time zone -- 逻辑删除时间
);
create index idx_providers_user_id on providers using btree (user_id);
comment on table public.providers is '服务提供商领域模型';
comment on column public.providers.id is '服务提供商ID';
comment on column public.providers.user_id is '用户ID';
comment on column public.providers.protocol is '协议类型';
comment on column public.providers.name is '服务提供商名称';
comment on column public.providers.description is '服务提供商描述';
comment on column public.providers.config is '服务提供商配置,加密后的值';
comment on column public.providers.is_official is '是否官方服务提供商';
comment on column public.providers.status is '服务提供商状态';
comment on column public.providers.created_at is '创建时间';
comment on column public.providers.updated_at is '更新时间';
comment on column public.providers.deleted_at is '逻辑删除时间';

create table public.rag_version_documents (
                                              id character varying(36) primary key not null, -- 主键ID
                                              rag_version_id character varying(36) not null, -- 关联RAG版本ID
                                              rag_version_file_id character varying(36) not null, -- 关联版本文件ID
                                              original_document_id character varying(36), -- 原始文档单元ID（仅标识）
                                              content text not null, -- 文档内容
                                              page integer, -- 页码
                                              vector_id character varying(36), -- 向量ID
                                              created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                                              updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                                              deleted_at timestamp without time zone -- 删除时间（软删除）
);
comment on table public.rag_version_documents is 'RAG版本文档单元表（文档内容快照）';
comment on column public.rag_version_documents.id is '主键ID';
comment on column public.rag_version_documents.rag_version_id is '关联RAG版本ID';
comment on column public.rag_version_documents.rag_version_file_id is '关联版本文件ID';
comment on column public.rag_version_documents.original_document_id is '原始文档单元ID（仅标识）';
comment on column public.rag_version_documents.content is '文档内容';
comment on column public.rag_version_documents.page is '页码';
comment on column public.rag_version_documents.vector_id is '向量ID';
comment on column public.rag_version_documents.created_at is '创建时间';
comment on column public.rag_version_documents.updated_at is '更新时间';
comment on column public.rag_version_documents.deleted_at is '删除时间（软删除）';

create table public.rag_version_files (
                                          id character varying(36) primary key not null, -- 主键ID
                                          rag_version_id character varying(36) not null, -- 关联RAG版本ID
                                          original_file_id character varying(36) not null, -- 原始文件ID（仅标识）
                                          file_name character varying(255) not null, -- 文件名
                                          file_size bigint default 0, -- 文件大小（字节）
                                          file_type character varying(50), -- 文件类型
                                          file_path character varying(500), -- 文件存储路径
                                          process_status integer, -- 处理状态
                                          embedding_status integer, -- 向量化状态
                                          created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                                          updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                                          deleted_at timestamp without time zone, -- 删除时间（软删除）
                                          file_page_size integer -- 文件页数
);
comment on table public.rag_version_files is 'RAG版本文件表（文件快照）';
comment on column public.rag_version_files.id is '主键ID';
comment on column public.rag_version_files.rag_version_id is '关联RAG版本ID';
comment on column public.rag_version_files.original_file_id is '原始文件ID（仅标识）';
comment on column public.rag_version_files.file_name is '文件名';
comment on column public.rag_version_files.file_size is '文件大小（字节）';
comment on column public.rag_version_files.file_type is '文件类型';
comment on column public.rag_version_files.file_path is '文件存储路径';
comment on column public.rag_version_files.process_status is '处理状态';
comment on column public.rag_version_files.embedding_status is '向量化状态';
comment on column public.rag_version_files.created_at is '创建时间';
comment on column public.rag_version_files.updated_at is '更新时间';
comment on column public.rag_version_files.deleted_at is '删除时间（软删除）';
comment on column public.rag_version_files.file_page_size is '文件页数';

create table public.rag_versions (
                                     id character varying(36) primary key not null, -- 主键ID
                                     name character varying(255) not null, -- 快照时的名称
                                     icon character varying(255), -- 快照时的图标
                                     description text, -- 快照时的描述
                                     user_id character varying(36) not null, -- 创建者用户ID
                                     version character varying(50) not null, -- 版本号 (如 "1.0.0")
                                     change_log text, -- 更新日志
                                     labels jsonb, -- 标签（JSON格式）
                                     original_rag_id character varying(36) not null, -- 原始RAG数据集ID（仅标识用）
                                     original_rag_name character varying(255), -- 原始RAG名称（快照时）
                                     file_count integer default 0, -- 文件数量
                                     total_size bigint default 0, -- 总大小（字节）
                                     document_count integer default 0, -- 文档单元数量
                                     publish_status integer default 1, -- 发布状态 1:审核中, 2:已发布, 3:拒绝, 4:已下架
                                     reject_reason text, -- 审核拒绝原因
                                     review_time timestamp without time zone, -- 审核时间
                                     published_at timestamp without time zone, -- 发布时间
                                     created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                                     updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                                     deleted_at timestamp without time zone -- 删除时间（软删除）
);
comment on table public.rag_versions is 'RAG版本表（完整快照）';
comment on column public.rag_versions.id is '主键ID';
comment on column public.rag_versions.name is '快照时的名称';
comment on column public.rag_versions.icon is '快照时的图标';
comment on column public.rag_versions.description is '快照时的描述';
comment on column public.rag_versions.user_id is '创建者用户ID';
comment on column public.rag_versions.version is '版本号 (如 "1.0.0")';
comment on column public.rag_versions.change_log is '更新日志';
comment on column public.rag_versions.labels is '标签（JSON格式）';
comment on column public.rag_versions.original_rag_id is '原始RAG数据集ID（仅标识用）';
comment on column public.rag_versions.original_rag_name is '原始RAG名称（快照时）';
comment on column public.rag_versions.file_count is '文件数量';
comment on column public.rag_versions.total_size is '总大小（字节）';
comment on column public.rag_versions.document_count is '文档单元数量';
comment on column public.rag_versions.publish_status is '发布状态 1:审核中, 2:已发布, 3:拒绝, 4:已下架';
comment on column public.rag_versions.reject_reason is '审核拒绝原因';
comment on column public.rag_versions.review_time is '审核时间';
comment on column public.rag_versions.published_at is '发布时间';
comment on column public.rag_versions.created_at is '创建时间';
comment on column public.rag_versions.updated_at is '更新时间';
comment on column public.rag_versions.deleted_at is '删除时间（软删除）';

create table public.rules (
                              id character varying(64) primary key not null,
                              name character varying(255) not null, -- 规则名称
                              handler_key character varying(100) not null, -- 处理器标识，对应策略枚举
                              description text, -- 规则描述
                              deleted_at timestamp without time zone,
                              created_at timestamp without time zone default CURRENT_TIMESTAMP,
                              updated_at timestamp without time zone default CURRENT_TIMESTAMP
);
comment on table public.rules is '计费规则表，存储不同的计费策略配置';
comment on column public.rules.name is '规则名称';
comment on column public.rules.handler_key is '处理器标识，对应策略枚举';
comment on column public.rules.description is '规则描述';

create table public.scheduled_tasks (
                                        id character varying(36) primary key not null, -- 定时任务唯一ID
                                        user_id character varying(36) not null, -- 用户ID
                                        agent_id character varying(36) not null, -- 关联的Agent ID
                                        session_id character varying(36) not null, -- 关联的会话ID
                                        content text not null, -- 任务内容
                                        repeat_type character varying(20) not null, -- 重复类型：NONE-不重复, DAILY-每天, WEEKLY-每周, MONTHLY-每月, WORKDAYS-工作日, CUSTOM-自定义
                                        repeat_config jsonb, -- 重复配置，JSON格式存储具体的重复规则
                                        status character varying(20) default 'ACTIVE', -- 任务状态：ACTIVE-活跃, PAUSED-暂停, COMPLETED-已完成
                                        last_execute_time timestamp without time zone, -- 上次执行时间
                                        next_execute_time timestamp without time zone,
                                        created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                                        updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                                        deleted_at timestamp without time zone -- 逻辑删除时间
);
create index idx_scheduled_tasks_user_id on scheduled_tasks using btree (user_id);
create index idx_scheduled_tasks_agent_id on scheduled_tasks using btree (agent_id);
create index idx_scheduled_tasks_session_id on scheduled_tasks using btree (session_id);
create index idx_scheduled_tasks_status on scheduled_tasks using btree (status);
comment on table public.scheduled_tasks is '定时任务实体类';
comment on column public.scheduled_tasks.id is '定时任务唯一ID';
comment on column public.scheduled_tasks.user_id is '用户ID';
comment on column public.scheduled_tasks.agent_id is '关联的Agent ID';
comment on column public.scheduled_tasks.session_id is '关联的会话ID';
comment on column public.scheduled_tasks.content is '任务内容';
comment on column public.scheduled_tasks.repeat_type is '重复类型：NONE-不重复, DAILY-每天, WEEKLY-每周, MONTHLY-每月, WORKDAYS-工作日, CUSTOM-自定义';
comment on column public.scheduled_tasks.repeat_config is '重复配置，JSON格式存储具体的重复规则';
comment on column public.scheduled_tasks.status is '任务状态：ACTIVE-活跃, PAUSED-暂停, COMPLETED-已完成';
comment on column public.scheduled_tasks.last_execute_time is '上次执行时间';
comment on column public.scheduled_tasks.created_at is '创建时间';
comment on column public.scheduled_tasks.updated_at is '更新时间';
comment on column public.scheduled_tasks.deleted_at is '逻辑删除时间';

create table public.sessions (
                                 id character varying(36) primary key not null, -- 会话唯一ID
                                 title character varying(255) not null, -- 会话标题
                                 user_id character varying(36) not null, -- 所属用户ID
                                 agent_id character varying(36), -- 关联的Agent版本ID
                                 description text, -- 会话描述
                                 is_archived boolean default false, -- 是否归档
                                 metadata jsonb, -- 会话元数据，可存储其他自定义信息，JSON格式
                                 created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                                 updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                                 deleted_at timestamp without time zone -- 逻辑删除时间
);
create index idx_sessions_user_id on sessions using btree (user_id);
create index idx_sessions_agent_id on sessions using btree (agent_id);
comment on table public.sessions is '会话实体类，代表一个独立的对话会话/主题';
comment on column public.sessions.id is '会话唯一ID';
comment on column public.sessions.title is '会话标题';
comment on column public.sessions.user_id is '所属用户ID';
comment on column public.sessions.agent_id is '关联的Agent版本ID';
comment on column public.sessions.description is '会话描述';
comment on column public.sessions.is_archived is '是否归档';
comment on column public.sessions.metadata is '会话元数据，可存储其他自定义信息，JSON格式';
comment on column public.sessions.created_at is '创建时间';
comment on column public.sessions.updated_at is '更新时间';
comment on column public.sessions.deleted_at is '逻辑删除时间';

create table public.tool_versions (
                                      id character varying(36) primary key not null, -- 版本唯一ID
                                      name character varying(255) not null, -- 工具名称
                                      icon character varying(255), -- 工具图标
                                      subtitle character varying(255), -- 副标题
                                      description text, -- 工具描述
                                      user_id character varying(36) not null, -- 用户ID
                                      version character varying(50) not null, -- 版本号
                                      tool_id character varying(36) not null, -- 工具ID
                                      upload_type character varying(20) not null, -- 上传方式
                                      change_log text,
                                      upload_url character varying(255), -- 上传URL
                                      tool_list jsonb, -- 工具列表，JSON数组格式
                                      labels jsonb, -- 标签列表，JSON数组格式
                                      mcp_server_name character varying(255),
                                      is_office boolean default false, -- 是否官方工具
                                      public_status boolean default false, -- 公开状态
                                      created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                                      updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                                      deleted_at timestamp without time zone -- 逻辑删除时间
);
comment on table public.tool_versions is '工具版本实体类';
comment on column public.tool_versions.id is '版本唯一ID';
comment on column public.tool_versions.name is '工具名称';
comment on column public.tool_versions.icon is '工具图标';
comment on column public.tool_versions.subtitle is '副标题';
comment on column public.tool_versions.description is '工具描述';
comment on column public.tool_versions.user_id is '用户ID';
comment on column public.tool_versions.version is '版本号';
comment on column public.tool_versions.tool_id is '工具ID';
comment on column public.tool_versions.upload_type is '上传方式';
comment on column public.tool_versions.upload_url is '上传URL';
comment on column public.tool_versions.tool_list is '工具列表，JSON数组格式';
comment on column public.tool_versions.labels is '标签列表，JSON数组格式';
comment on column public.tool_versions.is_office is '是否官方工具';
comment on column public.tool_versions.public_status is '公开状态';
comment on column public.tool_versions.created_at is '创建时间';
comment on column public.tool_versions.updated_at is '更新时间';
comment on column public.tool_versions.deleted_at is '逻辑删除时间';

create table public.tools (
                              id character varying(36) primary key not null, -- 工具唯一ID
                              name character varying(255) not null, -- 工具名称
                              icon character varying(255), -- 工具图标
                              subtitle character varying(255), -- 副标题
                              description text, -- 工具描述
                              user_id character varying(36) not null, -- 用户ID
                              labels jsonb, -- 标签列表，JSON数组格式
                              tool_type character varying(50) not null, -- 工具类型
                              upload_type character varying(20) not null, -- 上传方式
                              upload_url character varying(255), -- 上传URL
                              install_command jsonb, -- 安装命令，JSON格式
                              tool_list jsonb, -- 工具列表，JSON数组格式
                              reject_reason text,
                              failed_step_status character varying(20),
                              mcp_server_name character varying(255),
                              status character varying(20) not null, -- 审核状态
                              is_office boolean default false, -- 是否官方工具
                              created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                              updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                              deleted_at timestamp without time zone, -- 逻辑删除时间
                              is_global boolean not null default false -- 是否为全局工具（true=全局工具，在系统级别部署；false=用户工具，需要在用户容器中部署）
);
create index idx_tools_user_id on tools using btree (user_id);
comment on table public.tools is '工具实体类';
comment on column public.tools.id is '工具唯一ID';
comment on column public.tools.name is '工具名称';
comment on column public.tools.icon is '工具图标';
comment on column public.tools.subtitle is '副标题';
comment on column public.tools.description is '工具描述';
comment on column public.tools.user_id is '用户ID';
comment on column public.tools.labels is '标签列表，JSON数组格式';
comment on column public.tools.tool_type is '工具类型';
comment on column public.tools.upload_type is '上传方式';
comment on column public.tools.upload_url is '上传URL';
comment on column public.tools.install_command is '安装命令，JSON格式';
comment on column public.tools.tool_list is '工具列表，JSON数组格式';
comment on column public.tools.status is '审核状态';
comment on column public.tools.is_office is '是否官方工具';
comment on column public.tools.created_at is '创建时间';
comment on column public.tools.updated_at is '更新时间';
comment on column public.tools.deleted_at is '逻辑删除时间';
comment on column public.tools.is_global is '是否为全局工具（true=全局工具，在系统级别部署；false=用户工具，需要在用户容器中部署）';

create table public.usage_records (
                                      id character varying(64) primary key not null,
                                      user_id character varying(64) not null, -- 用户ID
                                      product_id character varying(64) not null, -- 商品ID
                                      quantity_data jsonb, -- 使用量数据（JSONB格式）
                                      cost numeric(20,8) not null, -- 本次消费金额
                                      request_id character varying(255) not null, -- 请求ID（幂等性保证）
                                      billed_at timestamp without time zone default CURRENT_TIMESTAMP, -- 计费时间
                                      deleted_at timestamp without time zone,
                                      created_at timestamp without time zone default CURRENT_TIMESTAMP,
                                      updated_at timestamp without time zone default CURRENT_TIMESTAMP,
                                      service_name character varying(255), -- 服务名称（如：GPT-4 模型调用）
                                      service_type character varying(100), -- 服务类型（如：模型服务）
                                      service_description text, -- 服务描述
                                      pricing_rule text, -- 定价规则说明（如：输入 ¥0.002/1K tokens，输出 ¥0.006/1K tokens）
                                      related_entity_name character varying(255) -- 关联实体名称（如：具体的模型名称或Agent名称）
);
comment on table public.usage_records is '使用记录表，存储用户的具体消费记录';
comment on column public.usage_records.user_id is '用户ID';
comment on column public.usage_records.product_id is '商品ID';
comment on column public.usage_records.quantity_data is '使用量数据（JSONB格式）';
comment on column public.usage_records.cost is '本次消费金额';
comment on column public.usage_records.request_id is '请求ID（幂等性保证）';
comment on column public.usage_records.billed_at is '计费时间';
comment on column public.usage_records.service_name is '服务名称（如：GPT-4 模型调用）';
comment on column public.usage_records.service_type is '服务类型（如：模型服务）';
comment on column public.usage_records.service_description is '服务描述';
comment on column public.usage_records.pricing_rule is '定价规则说明（如：输入 ¥0.002/1K tokens，输出 ¥0.006/1K tokens）';
comment on column public.usage_records.related_entity_name is '关联实体名称（如：具体的模型名称或Agent名称）';

create table public.user_containers (
                                        id character varying(36) primary key not null, -- 容器ID
                                        name character varying(100) not null, -- 容器名称
                                        user_id character varying(36) not null, -- 用户ID
                                        type character varying(255) not null,
                                        status integer not null, -- 容器状态: 1-创建中, 2-运行中, 3-已停止, 4-错误状态, 5-删除中, 6-已删除
                                        docker_container_id character varying(100), -- Docker容器ID
                                        image character varying(200) not null, -- 容器镜像
                                        internal_port integer not null, -- 内部端口
                                        external_port integer, -- 外部映射端口
                                        ip_address character varying(45), -- 容器IP地址
                                        cpu_usage numeric(5,2), -- CPU使用率(%)
                                        memory_usage numeric(5,2), -- 内存使用率(%)
                                        volume_path character varying(500), -- 数据卷路径
                                        env_config text, -- 环境变量配置(JSON)
                                        container_config text, -- 容器配置(JSON)
                                        error_message text, -- 错误信息
                                        created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                                        updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                                        deleted_at timestamp without time zone,
                                        last_accessed_at timestamp without time zone not null default CURRENT_TIMESTAMP -- 最后访问时间，用于自动清理判断
);
comment on table public.user_containers is '用户容器表';
comment on column public.user_containers.id is '容器ID';
comment on column public.user_containers.name is '容器名称';
comment on column public.user_containers.user_id is '用户ID';
comment on column public.user_containers.status is '容器状态: 1-创建中, 2-运行中, 3-已停止, 4-错误状态, 5-删除中, 6-已删除';
comment on column public.user_containers.docker_container_id is 'Docker容器ID';
comment on column public.user_containers.image is '容器镜像';
comment on column public.user_containers.internal_port is '内部端口';
comment on column public.user_containers.external_port is '外部映射端口';
comment on column public.user_containers.ip_address is '容器IP地址';
comment on column public.user_containers.cpu_usage is 'CPU使用率(%)';
comment on column public.user_containers.memory_usage is '内存使用率(%)';
comment on column public.user_containers.volume_path is '数据卷路径';
comment on column public.user_containers.env_config is '环境变量配置(JSON)';
comment on column public.user_containers.container_config is '容器配置(JSON)';
comment on column public.user_containers.error_message is '错误信息';
comment on column public.user_containers.created_at is '创建时间';
comment on column public.user_containers.updated_at is '更新时间';
comment on column public.user_containers.last_accessed_at is '最后访问时间，用于自动清理判断';

create table public.user_rag_documents (
                                           id character varying(36) primary key not null, -- 主键ID
                                           user_rag_id character varying(36) not null, -- 关联user_rags表的ID
                                           user_rag_file_id character varying(36) not null, -- 关联user_rag_files表的ID
                                           original_document_id character varying(36), -- 原始文档单元ID（仅用于标识，不依赖）
                                           content text not null, -- 文档内容（快照）
                                           page integer, -- 页码
                                           vector_id character varying(36), -- 向量ID（在向量数据库中的ID）
                                           created_at timestamp without time zone not null default CURRENT_TIMESTAMP,
                                           updated_at timestamp without time zone not null default CURRENT_TIMESTAMP,
                                           deleted_at timestamp without time zone
);
comment on table public.user_rag_documents is '用户RAG文档快照表 - 用于SNAPSHOT类型RAG的完全数据隔离';
comment on column public.user_rag_documents.id is '主键ID';
comment on column public.user_rag_documents.user_rag_id is '关联user_rags表的ID';
comment on column public.user_rag_documents.user_rag_file_id is '关联user_rag_files表的ID';
comment on column public.user_rag_documents.original_document_id is '原始文档单元ID（仅用于标识，不依赖）';
comment on column public.user_rag_documents.content is '文档内容（快照）';
comment on column public.user_rag_documents.page is '页码';
comment on column public.user_rag_documents.vector_id is '向量ID（在向量数据库中的ID）';

create table public.user_rag_files (
                                       id character varying(36) primary key not null, -- 主键ID
                                       user_rag_id character varying(36) not null, -- 关联user_rags表的ID
                                       original_file_id character varying(36) not null, -- 原始文件ID（仅用于标识，不依赖）
                                       file_name character varying(255) not null, -- 文件名（快照）
                                       file_size bigint default 0, -- 文件大小（字节）
                                       file_type character varying(50), -- 文件类型
                                       file_path character varying(500), -- 文件存储路径
                                       process_status integer, -- 处理状态（快照）
                                       embedding_status integer, -- 向量化状态（快照）
                                       created_at timestamp without time zone not null default CURRENT_TIMESTAMP,
                                       updated_at timestamp without time zone not null default CURRENT_TIMESTAMP,
                                       deleted_at timestamp without time zone,
                                       file_page_size integer default 0 -- 文件页数（快照）
);
comment on table public.user_rag_files is '用户RAG文件快照表 - 用于SNAPSHOT类型RAG的完全数据隔离';
comment on column public.user_rag_files.id is '主键ID';
comment on column public.user_rag_files.user_rag_id is '关联user_rags表的ID';
comment on column public.user_rag_files.original_file_id is '原始文件ID（仅用于标识，不依赖）';
comment on column public.user_rag_files.file_name is '文件名（快照）';
comment on column public.user_rag_files.file_size is '文件大小（字节）';
comment on column public.user_rag_files.file_type is '文件类型';
comment on column public.user_rag_files.file_path is '文件存储路径';
comment on column public.user_rag_files.process_status is '处理状态（快照）';
comment on column public.user_rag_files.embedding_status is '向量化状态（快照）';
comment on column public.user_rag_files.file_page_size is '文件页数（快照）';

create table public.user_rags (
                                  id character varying(36) primary key not null, -- 主键ID
                                  user_id character varying(36) not null, -- 用户ID
                                  rag_version_id character varying(36) not null, -- 关联的RAG版本快照ID
                                  name character varying(255) not null, -- 安装时的名称
                                  description text, -- 安装时的描述
                                  icon character varying(255), -- 安装时的图标
                                  version character varying(50) not null, -- 版本号
                                  installed_at timestamp without time zone default CURRENT_TIMESTAMP, -- 安装时间
                                  created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                                  updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                                  deleted_at timestamp without time zone, -- 删除时间（软删除）
                                  original_rag_id character varying(64), -- 原始RAG数据集ID
                                  install_type character varying(20) default 'SNAPSHOT' -- 安装类型：REFERENCE(引用)/SNAPSHOT(快照)
);
comment on table public.user_rags is '用户安装的RAG表';
comment on column public.user_rags.id is '主键ID';
comment on column public.user_rags.user_id is '用户ID';
comment on column public.user_rags.rag_version_id is '关联的RAG版本快照ID';
comment on column public.user_rags.name is '安装时的名称';
comment on column public.user_rags.description is '安装时的描述';
comment on column public.user_rags.icon is '安装时的图标';
comment on column public.user_rags.version is '版本号';
comment on column public.user_rags.installed_at is '安装时间';
comment on column public.user_rags.created_at is '创建时间';
comment on column public.user_rags.updated_at is '更新时间';
comment on column public.user_rags.deleted_at is '删除时间（软删除）';
comment on column public.user_rags.original_rag_id is '原始RAG数据集ID';
comment on column public.user_rags.install_type is '安装类型：REFERENCE(引用)/SNAPSHOT(快照)';

create table public.user_settings (
                                      id character varying(36) primary key not null, -- 设置记录唯一ID
                                      user_id character varying(36) not null, -- 用户ID，关联users表
                                      setting_config json, -- 设置配置JSON，格式：{"default_model": "模型ID"}
                                      created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                                      updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                                      deleted_at timestamp without time zone -- 逻辑删除时间
);
create unique index user_settings_user_id_key on user_settings using btree (user_id);
create index idx_user_settings_user_id on user_settings using btree (user_id);
comment on table public.user_settings is '用户设置表，存储用户的个性化配置';
comment on column public.user_settings.id is '设置记录唯一ID';
comment on column public.user_settings.user_id is '用户ID，关联users表';
comment on column public.user_settings.setting_config is '设置配置JSON，格式：{"default_model": "模型ID"}';
comment on column public.user_settings.created_at is '创建时间';
comment on column public.user_settings.updated_at is '更新时间';
comment on column public.user_settings.deleted_at is '逻辑删除时间';

create table public.user_tools (
                                   id character varying(36) primary key not null, -- 唯一ID
                                   user_id character varying(36) not null, -- 用户ID
                                   name character varying(255) not null, -- 工具名称
                                   description text, -- 工具描述
                                   icon character varying(255), -- 工具图标
                                   subtitle character varying(255), -- 副标题
                                   tool_id character varying(36) not null, -- 工具ID
                                   version character varying(50) not null, -- 版本号
                                   tool_list jsonb, -- 工具列表，JSON数组格式
                                   labels jsonb, -- 标签列表，JSON数组格式
                                   is_office boolean default false, -- 是否官方工具
                                   public_state boolean default false, -- 公开状态
                                   mcp_server_name character varying(255), -- MCP服务器名称
                                   created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                                   updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                                   deleted_at timestamp without time zone, -- 逻辑删除时间
                                   is_global boolean not null default false -- 是否为全局工具（继承自原始工具的全局状态）
);
comment on table public.user_tools is '用户工具关联实体类';
comment on column public.user_tools.id is '唯一ID';
comment on column public.user_tools.user_id is '用户ID';
comment on column public.user_tools.name is '工具名称';
comment on column public.user_tools.description is '工具描述';
comment on column public.user_tools.icon is '工具图标';
comment on column public.user_tools.subtitle is '副标题';
comment on column public.user_tools.tool_id is '工具ID';
comment on column public.user_tools.version is '版本号';
comment on column public.user_tools.tool_list is '工具列表，JSON数组格式';
comment on column public.user_tools.labels is '标签列表，JSON数组格式';
comment on column public.user_tools.is_office is '是否官方工具';
comment on column public.user_tools.public_state is '公开状态';
comment on column public.user_tools.mcp_server_name is 'MCP服务器名称';
comment on column public.user_tools.created_at is '创建时间';
comment on column public.user_tools.updated_at is '更新时间';
comment on column public.user_tools.deleted_at is '逻辑删除时间';
comment on column public.user_tools.is_global is '是否为全局工具（继承自原始工具的全局状态）';

create table public.users (
                              id character varying(36) primary key not null, -- 主键
                              nickname character varying(255) not null, -- 昵称
                              email character varying(255), -- 邮箱
                              phone character varying(11), -- 手机号
                              password character varying not null, -- 密码
                              is_admin boolean default false,
                              login_platform character varying(50),
                              created_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 创建时间
                              updated_at timestamp without time zone not null default CURRENT_TIMESTAMP, -- 更新时间
                              deleted_at timestamp without time zone, -- 逻辑删除时间
                              github_id character varying(255),
                              github_login character varying(255),
                              avatar_url character varying(255)
);
comment on column public.users.id is '主键';
comment on column public.users.nickname is '昵称';
comment on column public.users.email is '邮箱';
comment on column public.users.phone is '手机号';
comment on column public.users.password is '密码';
comment on column public.users.created_at is '创建时间';
comment on column public.users.updated_at is '更新时间';
comment on column public.users.deleted_at is '逻辑删除时间';



        -- 初始化认证配置数据
INSERT INTO auth_settings (id, feature_type, feature_key, feature_name, enabled, display_order, description) VALUES
                                                                                                                 ('auth-normal-login', 'LOGIN', 'NORMAL_LOGIN', '普通登录', TRUE, 1, '邮箱/手机号密码登录'),
                                                                                                                 ('auth-github-login', 'LOGIN', 'GITHUB_LOGIN', 'GitHub登录', TRUE, 2, 'GitHub OAuth登录'),
                                                                                                                 ('auth-community-login', 'LOGIN', 'COMMUNITY_LOGIN', '敲鸭登录', TRUE, 3, '敲鸭社区OAuth登录'),
                                                                                                                 ('auth-user-register', 'REGISTER', 'USER_REGISTER', '用户注册', TRUE, 1, '允许新用户注册账号');