# AgentX 前端项目

AgentX前端是基于Vue 3和PrimeVue构建的智能对话系统界面，提供与后端API的交互能力。

## 项目结构

```
AgentX-frontend/
├── public/          # 静态资源
├── src/             # 源代码
│   ├── assets/      # 静态资源
│   ├── components/  # 公共组件
│   ├── router/      # 路由配置
│   ├── stores/      # Pinia状态管理
│   ├── views/       # 视图组件
│   ├── App.vue      # 应用入口组件
│   └── main.js      # 应用入口
└── package.json     # 项目配置
```

## 技术栈

- Vue 3
- Pinia (状态管理)
- PrimeVue (UI组件库)
- Vue Router
- Axios (HTTP请求)

## 快速开始

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

### 构建生产版本

```bash
npm run build
```

## 连接后端

前端项目通过Axios与后端API进行通信，默认后端API地址为`http://localhost:8080/api`。

可以在`vite.config.js`中修改代理设置：

```js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

## 主要功能

- 对话界面：与AI模型进行实时对话
- 服务商选择：支持切换不同的AI服务提供商
- 响应式设计：适应不同尺寸的设备
