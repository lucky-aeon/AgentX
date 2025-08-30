import { useState } from "react"
import type { AgentVersion } from "@/types/agent"
import { getAgentVersions, getAgentLatestVersion } from "@/lib/agent-service"
import { toast } from "@/hooks/use-toast"

export function useAgentVersion() {
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [showPublishDialog, setShowPublishDialog] = useState(false)
  const [showVersionsDialog, setShowVersionsDialog] = useState(false)
  const [versionNumber, setVersionNumber] = useState("")
  const [changeLog, setChangeLog] = useState("")
  const [versions, setVersions] = useState<AgentVersion[]>([])
  const [selectedVersion, setSelectedVersion] = useState<AgentVersion | null>(null)
  const [latestVersion, setLatestVersion] = useState<AgentVersion | null>(null)

  // 获取助理最新版本
  const fetchLatestVersion = async (agentId: string) => {
    try {
      const response = await getAgentLatestVersion(agentId)
      
      if (response.code === 200) {
        setLatestVersion(response.data)
        
        // 如果有最新版本，预填写下一个版本号
        if (response.data && response.data.versionNumber) {
          const versionParts = response.data.versionNumber.split('.')
          if (versionParts.length >= 3) {
            // 增加补丁版本号
            const major = parseInt(versionParts[0])
            const minor = parseInt(versionParts[1])
            const patch = parseInt(versionParts[2]) + 1
            setVersionNumber(`${major}.${minor}.${patch}`)
          } else {
            // 无法解析版本号，设置为原版本号 + .1
            setVersionNumber(`${response.data.versionNumber}.1`)
          }
        } else {
          // 没有版本，设置初始版本号
          setVersionNumber("1.0.0")
        }
      } else {
        // 没有版本或获取失败，设置初始版本号
        setVersionNumber("1.0.0")
      }
    } catch (error) {
 
      // 出错，设置初始版本号
      setVersionNumber("1.0.0")
    }
  }

  // 加载助理版本列表
  const loadVersions = async (agentId: string) => {
    setVersions([])

    try {
      const response = await getAgentVersions(agentId)

      if (response.code === 200) {
        setVersions(response.data)
      } else {
        toast({
          title: "获取版本列表失败",
          description: response.message,
          variant: "destructive",
        })
      }
    } catch (error) {
 
      toast({
        title: "获取版本列表失败",
        description: "请稍后再试",
        variant: "destructive",
      })
    }
  }

  // 查看版本详情
  const viewVersionDetail = (version: AgentVersion) => {
    setSelectedVersion(version)
  }

  // 重置发布表单
  const resetPublishForm = () => {
    setVersionNumber("")
    setChangeLog("")
  }

  return {
    // 对话框状态
    showDeleteDialog,
    setShowDeleteDialog,
    showPublishDialog,
    setShowPublishDialog,
    showVersionsDialog,
    setShowVersionsDialog,
    
    // 版本数据
    versionNumber,
    setVersionNumber,
    changeLog,
    setChangeLog,
    versions,
    setVersions,
    selectedVersion,
    setSelectedVersion,
    latestVersion,
    setLatestVersion,
    
    // 方法
    fetchLatestVersion,
    loadVersions,
    viewVersionDetail,
    resetPublishForm,
  }
} 