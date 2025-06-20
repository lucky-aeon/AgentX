# 端口配置修正

## 问题说明

之前SSO请求使用了错误的端口（8088），现已修正为正确的端口配置。

## 修正内容

### 1. 后端端口修正
- **修改前**: `server.port: 8088`
- **修改后**: `server.port: 8080`
- **文件**: `AgentX/src/main/resources/application.yml`

### 2. 前端API配置修正
- **修改前**: `http://localhost:8088/api`
- **修改后**: `http://localhost:8080/api`
- **文件**: `agentx-frontend-plus/lib/api-config.ts`

### 3. SSO配置完善
- 在主配置文件中添加了完整的SSO配置
- 支持环境变量覆盖
- 支持内网穿透配置

## 当前正确的访问地址

### 开发环境
- **后端API**: http://localhost:8080/api
- **前端**: http://localhost:3000
- **SSO登录URL**: http://localhost:8080/api/sso/community/login
- **SSO回调URL**: http://localhost:3000/sso/community/callback

### 如果使用内网穿透
你可以通过环境变量配置：
```bash
SSO_COMMUNITY_CALLBACK_URL=https://your-tunnel-domain.com/sso/community/callback
```

## SSO流程说明

1. 前端请求: `GET http://localhost:8080/api/sso/community/login`
2. 后端返回Community登录URL: `https://7c7b630b.r6.cpolar.cn/sso/login?app_key=ai_project_001&redirect_url=...`
3. 用户在Community完成登录
4. Community回调: `http://localhost:3000/sso/community/callback?code=授权码`
5. 前端请求: `GET http://localhost:8080/api/sso/community/callback?code=授权码`
6. 后端返回JWT Token

## 验证状态

✅ 后端编译成功（端口8080）
✅ 前端API配置已更新
✅ SSO配置已完善
✅ 文档已更新

现在SSO功能应该能正常工作了！