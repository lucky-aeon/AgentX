"use client"

import { SmartContentDisplay, ContentDisplay } from "@/components/ui/content-display"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

export function TestContentDisplay() {
  // 测试数据
  const shortText = "这是一段短文本内容"
  
  const longText = "这是一段很长的文本内容，用来测试ContentDisplay组件的折叠功能。当内容超过设定的字符数限制时，组件应该会自动显示截断的预览，并提供展开按钮让用户查看完整内容。这个功能对于改善用户体验非常重要，特别是在显示大量数据时。"
  
  const jsonData = JSON.stringify({
    "toolName": "search",
    "parameters": {
      "query": "React组件开发",
      "limit": 10,
      "filters": ["technology", "frontend"]
    },
    "response": {
      "total": 156,
      "results": [
        {
          "title": "React组件最佳实践",
          "url": "https://example.com/react-best-practices",
          "description": "了解如何编写高质量的React组件"
        }
      ]
    }
  }, null, 2)

  return (
    <div className="p-6 space-y-6 max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold">ContentDisplay 组件测试</h1>
      
      {/* 短文本测试 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">短文本测试</CardTitle>
        </CardHeader>
        <CardContent>
          <SmartContentDisplay 
            content={shortText}
            maxLength={100}
          />
        </CardContent>
      </Card>

      {/* 长文本测试 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">长文本测试（阈值150字符）</CardTitle>
        </CardHeader>
        <CardContent>
          <SmartContentDisplay 
            content={longText}
            maxLength={150}
          />
        </CardContent>
      </Card>

      {/* JSON数据测试 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">JSON数据测试（自动格式化）</CardTitle>
        </CardHeader>
        <CardContent>
          <SmartContentDisplay 
            content={jsonData}
            maxLength={200}
          />
        </CardContent>
      </Card>

      {/* 工具入参出参模拟 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">工具调用参数模拟</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <h4 className="text-sm font-medium mb-2 text-muted-foreground">工具调用入参:</h4>
            <SmartContentDisplay 
              content='{"action": "search", "query": "React hooks 使用指南", "options": {"lang": "zh-CN", "max_results": 20}}'
              maxLength={100}
            />
          </div>
          
          <div>
            <h4 className="text-sm font-medium mb-2 text-muted-foreground">工具响应数据:</h4>
            <SmartContentDisplay 
              content={JSON.stringify({
                "status": "success",
                "data": {
                  "results": [
                    "React Hooks完全指南 - 深入理解useState, useEffect等核心hooks的使用方法和最佳实践",
                    "自定义Hooks开发 - 学习如何创建可复用的自定义hooks来简化组件逻辑"
                  ],
                  "total": 2
                },
                "execution_time": "245ms"
              }, null, 2)}
              maxLength={150}
            />
          </div>
        </CardContent>
      </Card>

      {/* 不同阈值测试 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">不同阈值测试</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <h4 className="text-sm font-medium mb-2">阈值50字符:</h4>
            <ContentDisplay 
              content={longText}
              maxLength={50}
              jsonFormat={false}
            />
          </div>
          
          <div>
            <h4 className="text-sm font-medium mb-2">阈值300字符:</h4>
            <ContentDisplay 
              content={longText}
              maxLength={300}
              jsonFormat={false}
            />
          </div>
        </CardContent>
      </Card>
    </div>
  )
}