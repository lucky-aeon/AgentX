"use client"

import { useState } from "react"
import { FallbackConfigComponent } from "@/components/settings/fallback-config"
import type { FallbackConfig, Model } from "@/lib/user-settings-service"

// 模拟模型数据
const mockModels: Model[] = [
  {
    id: "model-1",
    name: "GPT-4",
    providerId: "provider-1",
    providerName: "OpenAI",
    modelId: "gpt-4",
    type: "CHAT",
    status: true,
    isOfficial: true
  },
  {
    id: "model-2", 
    name: "GPT-3.5 Turbo",
    providerId: "provider-1",
    providerName: "OpenAI",
    modelId: "gpt-3.5-turbo",
    type: "CHAT",
    status: true,
    isOfficial: true
  },
  {
    id: "model-3",
    name: "Claude-3 Sonnet",
    providerId: "provider-2",
    providerName: "Anthropic", 
    modelId: "claude-3-sonnet",
    type: "CHAT",
    status: true,
    isOfficial: true
  },
  {
    id: "model-4",
    name: "Claude-3 Haiku",
    providerId: "provider-2",
    providerName: "Anthropic",
    modelId: "claude-3-haiku", 
    type: "CHAT",
    status: true,
    isOfficial: true
  }
]

export function TestFallbackConfig() {
  const [fallbackConfig, setFallbackConfig] = useState<FallbackConfig>({
    enabled: false,
    fallbackChain: []
  })

  const handleConfigChange = (config: FallbackConfig) => {
    setFallbackConfig(config)
    console.log("降级配置更新:", config)
  }

  return (
    <div className="container mx-auto py-6 space-y-6">
      <div>
        <h1 className="text-2xl font-bold">降级配置测试</h1>
        <p className="text-muted-foreground">测试拖拽功能和配置保存</p>
      </div>

      <FallbackConfigComponent
        fallbackConfig={fallbackConfig}
        models={mockModels}
        onConfigChange={handleConfigChange}
      />

      <div className="mt-6 p-4 bg-muted rounded-lg">
        <h3 className="font-medium mb-2">当前配置:</h3>
        <pre className="text-sm">
          {JSON.stringify(fallbackConfig, null, 2)}
        </pre>
      </div>
    </div>
  )
} 