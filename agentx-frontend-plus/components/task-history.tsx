"use client"

import { useState } from "react"
import { ChevronDown, ChevronRight, X, Check, Clock } from "lucide-react"
import { Button } from "@/components/ui/button"
import { ScrollArea } from "@/components/ui/scroll-area"

// 子任务类型
interface SubTask {
  id: string
  title: string
  status: "completed" | "in_progress" | "pending"
}

// 任务类型
interface Task {
  id: string
  title: string
  createdAt: string
  subTasks: SubTask[]
}

interface TaskHistoryProps {
  onClose: () => void
}

export function TaskHistory({ onClose }: TaskHistoryProps) {
  const [expandedTasks, setExpandedTasks] = useState<string[]>(["1"]) // 默认展开第一个任务

  // 模拟数据
  const tasks: Task[] = [
    {
      id: "1",
      title: "团队会议准备",
      createdAt: "1 分钟前",
      subTasks: [
        { id: "1-1", title: "创建会议议程模板", status: "completed" },
        { id: "1-2", title: "查找可用的会议室", status: "completed" },
        { id: "1-3", title: "发送会议邀请给团队成员", status: "completed" },
        { id: "1-4", title: "准备会议演示文稿", status: "in_progress" }
      ]
    },
    {
      id: "2",
      title: "市场分析报告",
      createdAt: "1 小时前",
      subTasks: [
        { id: "2-1", title: "收集市场数据", status: "completed" },
        { id: "2-2", title: "分析竞争对手策略", status: "completed" },
        { id: "2-3", title: "制作数据可视化图表", status: "in_progress" }
      ]
    },
    {
      id: "3",
      title: "产品发布活动",
      createdAt: "23 小时前",
      subTasks: [
        { id: "3-1", title: "确定发布日期", status: "completed" },
        { id: "3-2", title: "准备宣传材料", status: "completed" },
        { id: "3-3", title: "联系媒体合作伙伴", status: "in_progress" },
        { id: "3-4", title: "准备产品演示", status: "pending" }
      ]
    }
  ]

  // 切换任务展开/收起状态
  const toggleTaskExpand = (taskId: string) => {
    setExpandedTasks(prev => {
      if (prev.includes(taskId)) {
        return prev.filter(id => id !== taskId)
      } else {
        return [...prev, taskId]
      }
    })
  }

  // 获取子任务数量
  const getTaskCount = (task: Task) => {
    return `${task.subTasks.length}个任务`
  }

  // 获取任务状态统计
  const getInProgressCount = (task: Task) => {
    const inProgress = task.subTasks.filter(st => st.status === "in_progress").length
    const pending = task.subTasks.filter(st => st.status === "pending").length
    return inProgress + pending
  }

  return (
    <div className="border-l flex flex-col h-full bg-white w-[380px]">
      <div className="p-4 border-b flex items-center justify-between relative">
        <h2 className="text-lg font-semibold">历史任务</h2>
        <Button size="icon" variant="ghost" onClick={onClose}>
          <X className="h-4 w-4" />
          <span className="sr-only">关闭</span>
        </Button>
      </div>
      
      <ScrollArea className="flex-1">
        <div className="p-4 space-y-4">
          {tasks.map(task => {
            const inProgressCount = getInProgressCount(task)
            return (
              <div key={task.id} className="border rounded-md overflow-hidden">
                <div 
                  className="p-4 bg-gray-50 cursor-pointer"
                  onClick={() => toggleTaskExpand(task.id)}
                >
                  <div className="flex items-center justify-between mb-1">
                    <div className="flex items-center min-w-0 pr-2 flex-1">
                      <ChevronDown 
                        className={`h-4 w-4 mr-2 transition-transform flex-shrink-0 ${expandedTasks.includes(task.id) ? 'transform rotate-0' : 'transform rotate-[-90deg]'}`} 
                      />
                      <span className="font-medium truncate">{task.title}</span>
                    </div>
                    <div className="text-sm text-gray-500 whitespace-nowrap">
                      {getTaskCount(task)}
                    </div>
                  </div>
                  <div className="text-gray-500 text-xs ml-6 flex justify-between items-center">
                    <span>{task.createdAt}</span>
                    
                  </div>
                </div>
                
                {expandedTasks.includes(task.id) && (
                  <div className="border-t divide-y">
                    {task.subTasks.map(subTask => (
                      <div 
                        key={subTask.id} 
                        className="p-3 flex items-center justify-between hover:bg-gray-50 cursor-pointer"
                      >
                        <div className="flex items-center flex-1 min-w-0">
                          {subTask.status === 'completed' ? (
                            <div className="w-5 h-5 rounded-full bg-green-100 flex items-center justify-center mr-3 flex-shrink-0">
                              <Check className="h-3 w-3 text-green-600" />
                            </div>
                          ) : subTask.status === 'in_progress' ? (
                            <div className="w-5 h-5 rounded-full bg-blue-100 flex items-center justify-center mr-3 flex-shrink-0">
                              <Clock className="h-3 w-3 text-blue-600" />
                            </div>
                          ) : (
                            <div className="w-5 h-5 rounded-full bg-gray-100 flex items-center justify-center mr-3 flex-shrink-0">
                              <div className="h-2 w-2 rounded-full bg-gray-400"></div>
                            </div>
                          )}
                          <span className="truncate">{subTask.title}</span>
                        </div>
                        <div className="flex items-center ml-2">
                          <span className={`text-xs mr-2 px-2 py-1 rounded-full whitespace-nowrap ${
                            subTask.status === 'completed' 
                              ? 'bg-green-100 text-green-800' 
                              : subTask.status === 'in_progress'
                                ? 'bg-blue-100 text-blue-800'
                                : 'bg-gray-100 text-gray-800'
                          }`}>
                            {subTask.status === 'completed' 
                              ? '已完成' 
                              : subTask.status === 'in_progress'
                                ? '进行中'
                                : '待处理'}
                          </span>
                          <ChevronRight className="h-4 w-4 text-gray-400 flex-shrink-0" />
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )
          })}
        </div>
      </ScrollArea>
    </div>
  )
} 