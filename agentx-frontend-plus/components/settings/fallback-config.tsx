"use client"

import type React from "react"
import { useState } from "react"
import { DragDropContext, Droppable, Draggable, type DropResult } from "@hello-pangea/dnd"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import { Trash2, Plus, GripVertical } from "lucide-react"
import type { FallbackConfig, Model } from "@/lib/user-settings-service"

interface FallbackConfigProps {
  fallbackConfig: FallbackConfig
  models: Model[]
  onConfigChange: (config: FallbackConfig) => void
}

export function FallbackConfigComponent({ fallbackConfig, models, onConfigChange }: FallbackConfigProps) {
  const [selectedModelToAdd, setSelectedModelToAdd] = useState<string>("")

  // 获取模型名称
  const getModelName = (modelId: string) => {
    const model = models.find(m => m.id === modelId)
    return model ? `${model.name} (${model.providerName || 'Unknown Provider'})` : modelId
  }

  // 获取可添加的模型（排除已在链中的模型）
  const getAvailableModels = () => {
    return models.filter(model => !fallbackConfig.fallbackChain.includes(model.id))
  }

  // 处理启用/禁用降级
  const handleEnabledChange = (enabled: boolean) => {
    onConfigChange({
      ...fallbackConfig,
      enabled
    })
  }

  // 添加模型到降级链
  const handleAddModel = () => {
    if (!selectedModelToAdd) return

    onConfigChange({
      ...fallbackConfig,
      fallbackChain: [...fallbackConfig.fallbackChain, selectedModelToAdd]
    })
    setSelectedModelToAdd("")
  }

  // 从降级链中移除模型
  const handleRemoveModel = (modelId: string) => {
    onConfigChange({
      ...fallbackConfig,
      fallbackChain: fallbackConfig.fallbackChain.filter(id => id !== modelId)
    })
  }

  // 处理拖拽排序
  const handleDragEnd = (result: DropResult) => {
    if (!result.destination) return

    const newChain = Array.from(fallbackConfig.fallbackChain)
    const [reorderedItem] = newChain.splice(result.source.index, 1)
    newChain.splice(result.destination.index, 0, reorderedItem)

    onConfigChange({
      ...fallbackConfig,
      fallbackChain: newChain
    })
  }

  const availableModels = getAvailableModels()

  return (
    <Card>
      <CardHeader>
        <CardTitle>模型降级配置</CardTitle>
        <CardDescription>
          配置降级备选模型列表。当对话中使用的模型不可用时，系统将按照设定的顺序依次尝试这些降级模型
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* 启用开关 */}
        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <Label htmlFor="fallback-enabled">启用模型降级</Label>
            <div className="text-sm text-muted-foreground">
              开启后，当对话中使用的模型不可用时将自动切换到备用模型
            </div>
          </div>
          <Switch
            id="fallback-enabled"
            checked={fallbackConfig.enabled}
            onCheckedChange={handleEnabledChange}
          />
        </div>

        {/* 降级链配置 */}
        {fallbackConfig.enabled && (
          <div className="space-y-4">
            <div>
              <Label>降级模型列表</Label>
              <div className="text-sm text-muted-foreground mt-1 space-y-1">
                <div>当对话中使用的模型不可用时，系统将按以下顺序尝试降级模型</div>
                <div className="flex items-center gap-1 text-xs">
                  <GripVertical className="h-3 w-3" />
                  <span>拖拽整个卡片可调整优先级顺序</span>
                </div>
              </div>
            </div>

            {/* 添加模型 */}
            <div className="flex gap-2">
              <Select value={selectedModelToAdd} onValueChange={setSelectedModelToAdd}>
                <SelectTrigger className="flex-1">
                  <SelectValue placeholder="选择模型添加到降级列表" />
                </SelectTrigger>
                <SelectContent>
                  {availableModels.map((model) => (
                    <SelectItem key={model.id} value={model.id}>
                      {model.name} ({model.providerName || 'Unknown Provider'})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Button 
                onClick={handleAddModel} 
                disabled={!selectedModelToAdd}
                size="sm"
              >
                <Plus className="h-4 w-4 mr-1" />
                添加
              </Button>
            </div>

            {/* 降级链列表 */}
            {fallbackConfig.fallbackChain.length > 0 ? (
              <DragDropContext onDragEnd={handleDragEnd}>
                <Droppable droppableId="fallback-chain">
                  {(provided, snapshot) => (
                    <div
                      {...provided.droppableProps}
                      ref={provided.innerRef}
                      className={`space-y-2 transition-colors ${
                        snapshot.isDraggingOver ? 'bg-primary/5 rounded-lg p-2' : ''
                      }`}
                    >
                      {fallbackConfig.fallbackChain.map((modelId, index) => (
                        <Draggable key={modelId} draggableId={modelId} index={index}>
                          {(provided, snapshot) => (
                            <div
                              ref={provided.innerRef}
                              {...provided.draggableProps}
                              {...provided.dragHandleProps}
                              className={`flex items-center gap-3 p-3 border rounded-lg bg-background transition-all cursor-grab active:cursor-grabbing select-none ${
                                snapshot.isDragging 
                                  ? 'shadow-lg ring-2 ring-primary/20 scale-[1.02] rotate-1 bg-primary/5' 
                                  : 'hover:shadow-sm hover:border-primary/30 hover:bg-accent/50'
                              }`}
                            >
                              <div className="text-muted-foreground">
                                <GripVertical className="h-4 w-4" />
                              </div>
                              
                              <div className="flex items-center gap-2 flex-1">
                                <Badge variant="secondary" className="shrink-0">
                                  优先级 {index + 1}
                                </Badge>
                                <div className="flex flex-col min-w-0">
                                  <span className="text-sm font-medium truncate">{getModelName(modelId)}</span>
                                  <span className="text-xs text-muted-foreground">
                                    {index === 0 ? '首选降级模型' : `第 ${index + 1} 降级选择`}
                                  </span>
                                </div>
                              </div>

                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => handleRemoveModel(modelId)}
                                className="h-8 w-8 p-0 text-destructive hover:text-destructive hover:bg-destructive/10"
                              >
                                <Trash2 className="h-4 w-4" />
                              </Button>
                            </div>
                          )}
                        </Draggable>
                      ))}
                      {provided.placeholder}
                    </div>
                  )}
                </Droppable>
              </DragDropContext>
            ) : (
              <div className="text-center py-8 text-muted-foreground border-2 border-dashed border-muted rounded-lg">
                <div className="flex flex-col items-center gap-2">
                  <GripVertical className="h-8 w-8 text-muted-foreground/50" />
                  <div className="text-sm font-medium">暂无降级模型</div>
                  <div className="text-xs max-w-sm">
                    添加降级备选模型，当对话中使用的模型不可用时，系统将按优先级顺序自动尝试这些模型
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  )
} 