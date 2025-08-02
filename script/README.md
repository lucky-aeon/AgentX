# AgentX 数据库管理脚本

本目录包含用于管理 AgentX 项目的 PostgreSQL 数据库的脚本。

## 文件说明

- `setup_postgres.sh`: 使用 Docker 直接启动 PostgreSQL 容器的脚本
- `setup_with_compose.sh`: 使用 Docker Compose 启动 PostgreSQL 容器的脚本  
- `docker-compose.yml`: Docker Compose 配置文件

## SQL 文件位置

脚本会从 `docs/sql` 目录中读取 SQL 文件来初始化数据库。以下是默认的 SQL 文件：

- `init.sql`: 数据库初始化 SQL 脚本，包含表结构定义和索引创建

## 使用方法

### 方法一：使用直接的 Docker 命令

1. 确保已安装 Docker
2. 为脚本添加执行权限：
   ```bash
   chmod +x script/setup_postgres.sh
   ```
3. 运行脚本：
   ```bash
   ./script/setup_postgres.sh
   ```

### 方法二：使用 Docker Compose

1. 确保已安装 Docker 和 Docker Compose
2. 为脚本添加执行权限：
   ```bash
   chmod +x script/setup_with_compose.sh
   ```
3. 运行脚本：
   ```bash
   ./script/setup_with_compose.sh
   ```

## 数据库连接信息

- 主机：`localhost`
- 端口：`5432`
- 用户名：`postgres`
- 密码：`postgres`
- 数据库名：`agentx`
- JDBC URL：`jdbc:postgresql://localhost:5432/agentx`

## 常用命令

### 连接到数据库

```bash
docker exec -it agentx-postgres psql -U postgres -d agentx
```

### 查看容器状态

```bash
docker ps
```

### 查看容器日志

```bash
docker logs agentx-postgres
```

### 停止并删除容器（使用 Docker Compose）

```bash
cd script
docker-compose down
# 或者（根据 Docker 版本）
docker compose down
```

### 停止并删除容器（使用 Docker 命令）

```bash
docker stop agentx-postgres
docker rm agentx-postgres
```

## 注意事项

- 脚本会自动检查并处理已存在的容器和端口占用情况
- 使用的是 `ankane/pgvector:latest` 镜像，支持向量搜索功能
- 数据会持久化到 Docker 卷中，容器删除后数据不会丢失 