# 数据库脚本说明

本目录包含AgentX项目的数据库脚本文件，用于创建和维护数据库结构。

## 文件说明

- `schema_main.sql` - 主数据库脚本，包含会话、消息和上下文的表结构及索引
- `schema_message_group.sql` - 消息分组功能脚本，包含话题关联表结构及索引

## 使用方法

### 安装基础功能

首先创建基础数据库结构：

```bash
psql -U postgres -d agentx -f schema_main.sql
```

### 安装消息分组功能

如果需要启用消息分组功能，执行：

```bash
psql -U postgres -d agentx -f schema_message_group.sql
```

> 注意：必须先执行`schema_main.sql`，再执行`schema_message_group.sql`，因为后者依赖于前者创建的sessions表。

## 数据库设计文档

更详细的数据库设计文档请参考 `database_schema.md`。 