# AgentX Frontend Plus

## 项目概述

AgentX的前端项目，基于Next.js开发。

## 技术栈

- Next.js
- React
- TypeScript
- Tailwind CSS
- Shadcn/ui

## API响应Toast提示功能

### 功能介绍

为了提升用户体验，我们在API请求成功或失败时会自动展示Toast消息。API响应的格式如下：

```typescript
{
    "code": 200,
    "message": "操作成功",
    "data": null,
    "timestamp": 1742872711904
}
```

### 使用方法

#### 1. 直接使用带Toast的API函数

```typescript
import { createSessionWithToast } from "@/lib/api-services";

// 在组件中使用
async function handleCreateSession() {
  try {
    // 自动处理成功/失败提示
    const response = await createSessionWithToast({ title: "新会话" });
    
    if (response.code === 200) {
      // 处理成功后的逻辑
    }
  } catch (error) {
    // 额外的错误处理（可选）
  }
}
```

#### 2. 使用handleApiResponse包装已有的接口响应

```typescript
import { handleApiResponse } from "@/lib/toast-utils";
import { createSession } from "@/lib/api-services";

async function handleCreateSession() {
  try {
    const response = await createSession({ title: "新会话" });
    
    // 手动处理Toast显示
    handleApiResponse(response, {
      successTitle: "自定义成功标题",
      errorTitle: "自定义错误标题"
    });
    
    if (response.code === 200) {
      // 处理成功后的逻辑
    }
  } catch (error) {
    // 错误处理
  }
}
```

#### 3. 使用withToast高阶函数包装自定义API函数

```typescript
import { withToast } from "@/lib/toast-utils";

// 自定义API函数
async function customApiCall(): Promise<ApiResponse<any>> {
  // 实现逻辑...
}

// 包装为带Toast的函数
const customApiCallWithToast = withToast(customApiCall, {
  successTitle: "自定义操作成功",
  errorTitle: "自定义操作失败"
});

// 使用
async function handleCustomAction() {
  const response = await customApiCallWithToast();
  // 处理逻辑...
}
```

### 配置选项

`handleApiResponse`和`withToast`函数支持以下配置选项：

- `showSuccessToast`: 是否显示成功提示，默认为`true`
- `showErrorToast`: 是否显示错误提示，默认为`true`
- `successTitle`: 成功提示的标题，默认为`"操作成功"`
- `errorTitle`: 错误提示的标题，默认为`"操作失败"`

### 注意事项

1. 对于不需要展示成功提示的API（如获取列表等），已经预设`showSuccessToast: false`
2. Toast会自动使用API响应中的`message`字段作为提示内容
3. 错误状态的Toast会使用红色样式 