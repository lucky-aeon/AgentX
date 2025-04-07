"use client"

import React, { useState } from 'react';
import { ChevronDown, ChevronRight, CheckCircle, Circle, Clock, FileText } from 'lucide-react';
import { Badge } from "@/components/ui/badge";
import { cn } from '@/lib/utils';

// 定义任务状态类型
type TaskStatus = 'completed' | 'in-progress' | 'pending';

// 任务接口
interface Task {
  id: string;
  title: string;
  status: TaskStatus;
}

// 默认任务数据
const defaultTaskName = "PDF文档分析任务";
const defaultTasks: Task[] = [
  { id: '1', title: '分析用户上传的PDF文档', status: 'completed' },
  { id: '2', title: '提取文档中的关键信息', status: 'completed' },
  { id: '3', title: '识别文档中的表格数据', status: 'in-progress' },
  { id: '4', title: '生成数据分析报告', status: 'pending' },
  { id: '5', title: '转换数据格式为CSV', status: 'pending' },
  { id: '6', title: '整理分析结果', status: 'pending' },
  { id: '7', title: '创建可视化图表', status: 'pending' },
  { id: '8', title: '总结关键发现', status: 'pending' },
];

interface CurrentTaskListProps {
  taskName?: string;
  tasks?: Task[];
}

export function CurrentTaskList({ taskName = defaultTaskName, tasks = defaultTasks }: CurrentTaskListProps) {
  // 默认设置为关闭状态(true表示已折叠)
  const [isCollapsed, setIsCollapsed] = useState(true);

  // 计算各状态任务数量
  const completedCount = tasks.filter(task => task.status === 'completed').length;
  const inProgressCount = tasks.filter(task => task.status === 'in-progress').length;
  const pendingCount = tasks.filter(task => task.status === 'pending').length;

  // 根据任务状态获取相应的图标
  const getStatusIcon = (status: TaskStatus) => {
    switch (status) {
      case 'completed':
        return <CheckCircle className="h-4 w-4 text-green-500" />;
      case 'in-progress':
        return <Clock className="h-4 w-4 text-blue-500" />;
      case 'pending':
        return <Circle className="h-4 w-4 text-gray-400" />;
    }
  };

  // 根据任务状态获取相应的标签
  const getStatusBadge = (status: TaskStatus) => {
    switch (status) {
      case 'completed':
        return <Badge variant="outline" className="bg-green-50 text-green-700 border-green-200">已完成</Badge>;
      case 'in-progress':
        return <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200">进行中</Badge>;
      case 'pending':
        return <Badge variant="outline" className="bg-gray-50 text-gray-700 border-gray-200">待处理</Badge>;
    }
  };

  if (!tasks || tasks.length === 0) {
    return null;
  }

  return (
    <div className="rounded-lg border shadow-sm w-full">
      {/* 任务名称 */}
      <div className="bg-blue-50 p-3 rounded-t-lg border-b border-blue-100 flex items-center">
        <FileText className="h-4 w-4 text-blue-600 mr-2" />
        <h3 className="font-medium text-blue-700">{taskName}</h3>
      </div>
      
      {/* 标题栏 */}
      <div 
        className="flex items-center justify-between p-2 bg-gray-50 cursor-pointer border-b"
        onClick={() => setIsCollapsed(!isCollapsed)}
      >
        <div className="flex items-center space-x-2 overflow-x-auto">
          {isCollapsed ? <ChevronRight className="h-4 w-4 flex-shrink-0" /> : <ChevronDown className="h-4 w-4 flex-shrink-0" />}
          <span className="font-medium whitespace-nowrap">子任务列表</span>
          <div className="flex space-x-2 ml-2 flex-wrap">
            <Badge variant="outline" className="bg-gray-50 whitespace-nowrap">
              总计: {tasks.length}
            </Badge>
            {completedCount > 0 && (
              <Badge variant="outline" className="bg-green-50 text-green-700 border-green-200 whitespace-nowrap">
                已完成: {completedCount}
              </Badge>
            )}
            {inProgressCount > 0 && (
              <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200 whitespace-nowrap">
                进行中: {inProgressCount}
              </Badge>
            )}
            {pendingCount > 0 && (
              <Badge variant="outline" className="bg-gray-50 text-gray-700 border-gray-200 whitespace-nowrap">
                待处理: {pendingCount}
              </Badge>
            )}
          </div>
        </div>
      </div>
      
      {/* 任务列表 */}
      {!isCollapsed && (
        <div 
          className="max-h-[250px] overflow-y-auto bg-white rounded-b-lg"
        >
          {tasks.map((task) => (
            <div 
              key={task.id}
              className="flex items-center justify-between p-2 border-t hover:bg-gray-50"
            >
              <div className="flex items-center space-x-2 min-w-0 flex-1">
                <span className="flex-shrink-0">{getStatusIcon(task.status)}</span>
                <span className={cn(
                  "text-sm truncate",
                  task.status === 'completed' && "text-gray-500 line-through"
                )}>
                  {task.title}
                </span>
              </div>
              <div className="flex items-center space-x-2 flex-shrink-0">
                {getStatusBadge(task.status)}
                <ChevronRight className="h-4 w-4 text-gray-400" />
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
} 