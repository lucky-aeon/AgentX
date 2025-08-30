"use client"

import { useState } from "react"
import { Copy, ChevronDown, ChevronUp, Key, Code, Book, Shield } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
import { toast } from "@/hooks/use-toast"

export function ApiUsageGuide() {
  const [isExpanded, setIsExpanded] = useState(false)

  const copyToClipboard = (text: string, description: string) => {
    navigator.clipboard.writeText(text)
    toast({
      title: "已复制到剪贴板",
      description,
    })
  }

  const curlExample = `curl -X POST https://api.agentx.ai/v1/chat/completions \\
  -H "Authorization: Bearer YOUR_API_KEY" \\
  -H "Content-Type: application/json" \\
  -d '{
    "message": "Hello, world!",
    "stream": false
  }'`

  const pythonExample = `import requests

headers = {
    "Authorization": "Bearer YOUR_API_KEY",
    "Content-Type": "application/json"
}

data = {
    "message": "Hello, world!",
    "stream": False
}

response = requests.post(
    "https://api.agentx.ai/v1/chat/completions",
    headers=headers,
    json=data
)

result = response.json()
print(result)`

  const nodejsExample = `const axios = require('axios');

const headers = {
  'Authorization': 'Bearer YOUR_API_KEY',
  'Content-Type': 'application/json'
};

const data = {
  message: 'Hello, world!',
  stream: false
};

axios.post('https://api.agentx.ai/v1/chat/completions', data, { headers })
  .then(response => {
 
  })
  .catch(error => {
 
  });`

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Book className="h-5 w-5" />
          API 使用指南
        </CardTitle>
        <CardDescription>了解如何使用 API 密钥</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* 基本认证信息 */}
        <div>
          <h3 className="font-medium mb-3 flex items-center gap-2">
            <Shield className="h-4 w-4" />
            身份验证
          </h3>
          <div className="bg-muted p-3 rounded-md">
            <pre className="text-sm overflow-x-auto">
              <code>Authorization: Bearer YOUR_API_KEY</code>
            </pre>
            <Button
              variant="ghost"
              size="sm"
              className="mt-2"
              onClick={() => copyToClipboard("Authorization: Bearer YOUR_API_KEY", "认证头已复制")}
            >
              <Copy className="mr-2 h-4 w-4" />
              复制认证头
            </Button>
          </div>
        </div>

        {/* 快速开始 */}
        <div>
          <h3 className="font-medium mb-3">快速开始</h3>
          <div className="bg-muted p-3 rounded-md">
            <pre className="text-sm overflow-x-auto whitespace-pre-wrap">
              <code>{curlExample}</code>
            </pre>
            <Button
              variant="ghost"
              size="sm"
              className="mt-2"
              onClick={() => copyToClipboard(curlExample, "cURL 示例已复制")}
            >
              <Copy className="mr-2 h-4 w-4" />
              复制 cURL 示例
            </Button>
          </div>
        </div>

        {/* 可折叠的详细示例 */}
        <Collapsible open={isExpanded} onOpenChange={setIsExpanded}>
          <CollapsibleTrigger asChild>
            <Button variant="outline" className="w-full justify-between">
              <span className="flex items-center gap-2">
                <Code className="h-4 w-4" />
                查看更多代码示例
              </span>
              {isExpanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
            </Button>
          </CollapsibleTrigger>
          <CollapsibleContent className="space-y-4 mt-4">
            {/* Python 示例 */}
            <div>
              <h4 className="font-medium mb-2">Python 示例</h4>
              <div className="bg-muted p-3 rounded-md">
                <pre className="text-sm overflow-x-auto whitespace-pre-wrap">
                  <code>{pythonExample}</code>
                </pre>
                <Button
                  variant="ghost"
                  size="sm"
                  className="mt-2"
                  onClick={() => copyToClipboard(pythonExample, "Python 示例已复制")}
                >
                  <Copy className="mr-2 h-4 w-4" />
                  复制 Python 代码
                </Button>
              </div>
            </div>

            {/* Node.js 示例 */}
            <div>
              <h4 className="font-medium mb-2">Node.js 示例</h4>
              <div className="bg-muted p-3 rounded-md">
                <pre className="text-sm overflow-x-auto whitespace-pre-wrap">
                  <code>{nodejsExample}</code>
                </pre>
                <Button
                  variant="ghost"
                  size="sm"
                  className="mt-2"
                  onClick={() => copyToClipboard(nodejsExample, "Node.js 示例已复制")}
                >
                  <Copy className="mr-2 h-4 w-4" />
                  复制 Node.js 代码
                </Button>
              </div>
            </div>

            {/* API 端点 */}
            <div>
              <h4 className="font-medium mb-2">可用端点</h4>
              <div className="space-y-2 text-sm">
                <div className="flex items-center justify-between p-2 bg-muted rounded">
                  <code>POST /v1/chat/completions</code>
                  <span className="text-muted-foreground">发起对话</span>
                </div>
                <div className="flex items-center justify-between p-2 bg-muted rounded">
                  <code>GET /v1/models</code>
                  <span className="text-muted-foreground">获取可用模型</span>
                </div>
                <div className="flex items-center justify-between p-2 bg-muted rounded">
                  <code>GET /v1/sessions</code>
                  <span className="text-muted-foreground">获取会话列表</span>
                </div>
                <div className="flex items-center justify-between p-2 bg-muted rounded">
                  <code>POST /v1/sessions</code>
                  <span className="text-muted-foreground">创建新会话</span>
                </div>
              </div>
            </div>
          </CollapsibleContent>
        </Collapsible>

        {/* 重要提示 */}
        <div>
          <h3 className="font-medium mb-3">重要提示</h3>
          <div className="space-y-2 text-sm text-muted-foreground">
            <div className="flex items-start gap-2">
              <span className="font-medium text-foreground">速率限制：</span>
              <span>根据您的计划，API 请求有不同的速率限制。</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="font-medium text-foreground">安全性：</span>
              <span>请勿在客户端代码中暴露 API 密钥，应在服务器端使用。</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="font-medium text-foreground">监控：</span>
              <span>建议定期检查 API 密钥的使用情况和安全状态。</span>
            </div>
          </div>
        </div>

        {/* 文档链接 */}
        <div className="flex items-center gap-2 pt-4 border-t">
          <Button variant="outline" className="gap-2">
            <Key className="h-4 w-4" />
            完整 API 文档
          </Button>
          <Button variant="outline" className="gap-2">
            <Code className="h-4 w-4" />
            SDK 下载
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}