# 安全配置指南

## 重要提醒

⚠️ **敏感信息已从代码库中移除**

为了安全起见，所有敏感的配置信息（如API密钥、密码等）都不应提交到git仓库中。

## 配置方法

### 方法1：环境变量（推荐）

复制 `.env.example` 为 `.env` 并填入真实值：

```bash
cp .env.example .env
# 编辑 .env 文件，填入真实的配置值
```

### 方法2：系统环境变量

在系统中设置环境变量：

```bash
export SSO_COMMUNITY_APP_KEY="your_real_app_key"
export SSO_COMMUNITY_APP_SECRET="your_real_app_secret"
export GITHUB_CLIENT_ID="your_github_client_id"
export GITHUB_CLIENT_SECRET="your_github_client_secret"
```

### 方法3：IDE运行配置

在IDE中设置环境变量（如IntelliJ IDEA的Run Configuration）

## 敏感配置项

以下配置项包含敏感信息，需要通过环境变量设置：

- `SSO_COMMUNITY_APP_KEY` - 敲鸭应用标识
- `SSO_COMMUNITY_APP_SECRET` - 敲鸭应用密钥  
- `GITHUB_CLIENT_ID` - GitHub OAuth Client ID
- `GITHUB_CLIENT_SECRET` - GitHub OAuth Client Secret

## Git安全措施

1. `.gitignore` 已配置忽略包含敏感信息的文件
2. 配置文件中使用环境变量占位符
3. 提供 `.env.example` 作为配置模板

## 注意事项

- 永远不要将真实的密钥提交到git
- 定期轮换密钥以提高安全性
- 不同环境使用不同的密钥（开发/测试/生产）