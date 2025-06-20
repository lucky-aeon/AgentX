# AgentX SSO 集成配置指南

## 概述

AgentX已实现通用SSO架构，支持多种SSO提供商。目前已集成敲鸭(Qiaoya) SSO，未来可轻松扩展支持GitHub、Google等其他提供商。

## 架构设计

### 后端架构
- **SsoService接口**: 定义SSO提供商的通用操作接口
- **SsoProvider枚举**: 定义支持的SSO提供商类型
- **SsoServiceFactory**: 管理和创建不同的SSO服务实例
- **CommunitySsoService**: 敲鸭 SSO的具体实现
- **SsoController**: 统一的SSO控制器，处理所有SSO请求
- **SsoAppService**: SSO应用服务，处理业务逻辑

### 前端架构
- **SSO登录按钮**: 支持多种SSO提供商的登录界面
- **SSO回调页面**: 统一处理不同SSO提供商的回调
- **API服务**: 封装SSO相关的API调用

## 配置说明

### 1. 后端配置

**端口配置**：AgentX后端运行在 **8080** 端口

在 `application.yml` 中已包含以下SSO配置：

```yaml
sso:
  community:
    base-url: https://7c7b630b.r6.cpolar.cn
    app-key: ai_project_001
    app-secret: ai_secret_987654321
    callback-url: http://localhost:3000/sso/community/callback
```

**使用环境变量覆盖**：

```bash
SSO_COMMUNITY_BASE_URL=https://7c7b630b.r6.cpolar.cn
SSO_COMMUNITY_APP_KEY=ai_project_001
SSO_COMMUNITY_APP_SECRET=ai_secret_987654321
SSO_COMMUNITY_CALLBACK_URL=http://localhost:3000/sso/community/callback
```

**如果使用内网穿透**，可以设置：
```bash
SSO_COMMUNITY_CALLBACK_URL=https://your-tunnel-domain.com/sso/community/callback
```

### 2. 前端配置

前端会自动使用后端提供的SSO接口，无需额外配置。

## API接口

### 1. 获取SSO登录URL
```http
GET /sso/{provider}/login?redirectUrl={redirectUrl}
```

参数：
- `provider`: SSO提供商代码（如：community、github）
- `redirectUrl`: 可选，登录成功后的重定向地址

响应：
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "loginUrl": "https://7c7b630b.r6.cpolar.cn/sso/login?app_key=ai_project_001&redirect_url=..."
  }
}
```

### 2. SSO回调处理
```http
GET /sso/{provider}/callback?code={authCode}
```

参数：
- `provider`: SSO提供商代码
- `code`: SSO授权码

响应：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "jwt_token_here"
  }
}
```

## 登录流程

1. **发起登录**: 前端调用 `/sso/{provider}/login` 获取登录URL
2. **用户授权**: 重定向到SSO提供商进行登录授权
3. **回调处理**: SSO提供商回调到 `/sso/{provider}/callback`
4. **获取用户信息**: 后端通过授权码获取用户信息
5. **创建本地用户**: 如果用户不存在则创建，存在则更新信息
6. **返回Token**: 生成JWT Token返回给前端

## 扩展新的SSO提供商

### 1. 添加枚举值
在 `SsoProvider` 枚举中添加新的提供商：

```java
GOOGLE("google", "Google"),
WECHAT("wechat", "微信");
```

### 2. 实现SsoService接口
创建具体的SSO服务实现：

```java
@Service
public class GoogleSsoService implements SsoService {
    @Override
    public String getLoginUrl(String redirectUrl) {
        // 实现Google登录URL生成逻辑
    }
    
    @Override
    public SsoUserInfo getUserInfo(String authCode) {
        // 实现Google用户信息获取逻辑
    }
    
    @Override
    public SsoProvider getProvider() {
        return SsoProvider.GOOGLE;
    }
}
```

### 3. 添加配置
在配置文件中添加新提供商的配置项。

### 4. 前端适配
在登录页面添加新的SSO登录按钮。

## 安全说明

1. **密钥安全**: `app_secret` 仅在后端使用，不会暴露给前端
2. **授权码验证**: 授权码有时效性（5分钟）且只能使用一次
3. **HTTPS**: 生产环境必须使用HTTPS
4. **Token管理**: JWT Token存储在localStorage和Cookie中

## 测试

1. 启动后端服务（端口8080）
2. 启动前端服务（端口3000）
3. 访问登录页面
4. 点击"使用Community登录"按钮
5. 在Community平台完成登录
6. 验证是否成功跳转回AgentX并获得Token

## 故障排除

1. **配置检查**: 确保SSO配置正确
2. **网络连接**: 确保能访问Community SSO服务
3. **回调地址**: 确保回调地址与配置一致
4. **日志查看**: 查看后端日志获取详细错误信息

## 敲鸭 SSO特定信息

- **服务地址**: http://localhost:8080 (本地开发)
- **应用标识**: ai_project_001
- **应用密钥**: ai_secret_987654321
- **回调地址**: http://localhost:3000/sso/community/callback（开发环境）
- **Logo**: /public/logo.jpg