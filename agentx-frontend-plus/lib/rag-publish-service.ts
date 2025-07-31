import { httpClient } from './http-client'
import { toast } from '@/hooks/use-toast'
import type {
  RagVersionDTO,
  UserRagDTO,
  RagMarketDTO,
  PublishRagRequest,
  InstallRagRequest,
  ReviewRagVersionRequest,
  PageResponse,
  ApiResponse,
  QueryParams
} from '@/types/rag-publish'

// API 端点
const API_ENDPOINTS = {
  PUBLISH: '/rag/publish',
  MARKET: '/rag/market',
  ADMIN_REVIEW: '/admin/rag/review'
} as const

// ================================ RAG发布相关接口 ================================

/** 发布RAG版本 */
export async function publishRagVersion(data: PublishRagRequest): Promise<ApiResponse<RagVersionDTO>> {
  try {
    return await httpClient.post<ApiResponse<RagVersionDTO>>(API_ENDPOINTS.PUBLISH, data)
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "发布失败",
      data: {} as RagVersionDTO,
      timestamp: Date.now()
    }
  }
}

/** 发布RAG版本（带Toast提示） */
export async function publishRagVersionWithToast(data: PublishRagRequest): Promise<ApiResponse<RagVersionDTO>> {
  const response = await publishRagVersion(data)
  
  if (response.code === 200) {
    toast({
      title: "发布成功",
      description: "RAG版本已提交审核",
    })
  } else {
    toast({
      title: "发布失败",
      description: response.message,
      variant: "destructive"
    })
  }
  
  return response
}

/** 获取用户的RAG版本列表 */
export async function getUserRagVersions(params?: QueryParams): Promise<ApiResponse<PageResponse<RagVersionDTO>>> {
  try {
    return await httpClient.get<ApiResponse<PageResponse<RagVersionDTO>>>(
      `${API_ENDPOINTS.PUBLISH}/versions`,
      { params }
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "获取版本列表失败",
      data: { records: [], total: 0, size: 15, current: 1, pages: 0 },
      timestamp: Date.now()
    }
  }
}

/** 获取用户的RAG版本列表（带Toast提示） */
export async function getUserRagVersionsWithToast(params?: QueryParams): Promise<ApiResponse<PageResponse<RagVersionDTO>>> {
  const response = await getUserRagVersions(params)
  
  if (response.code !== 200) {
    toast({
      title: "获取版本列表失败",
      description: response.message,
      variant: "destructive"
    })
  }
  
  return response
}

/** 获取RAG版本历史 */
export async function getRagVersionHistory(ragId: string): Promise<ApiResponse<RagVersionDTO[]>> {
  try {
    return await httpClient.get<ApiResponse<RagVersionDTO[]>>(
      `${API_ENDPOINTS.PUBLISH}/versions/history/${ragId}`
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "获取版本历史失败",
      data: [],
      timestamp: Date.now()
    }
  }
}

/** 获取RAG版本详情 */
export async function getRagVersionDetail(versionId: string): Promise<ApiResponse<RagVersionDTO>> {
  try {
    return await httpClient.get<ApiResponse<RagVersionDTO>>(
      `${API_ENDPOINTS.PUBLISH}/versions/${versionId}`
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "获取版本详情失败",
      data: {} as RagVersionDTO,
      timestamp: Date.now()
    }
  }
}

/** 获取RAG数据集的最新版本号 */
export async function getLatestVersionNumber(ragId: string): Promise<ApiResponse<string>> {
  try {
    return await httpClient.get<ApiResponse<string>>(
      `${API_ENDPOINTS.PUBLISH}/versions/latest/${ragId}`
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "获取最新版本号失败",
      data: null,
      timestamp: Date.now()
    }
  }
}

// ================================ RAG市场相关接口 ================================

/** 获取市场上的RAG版本列表 */
export async function getMarketRagVersions(params?: QueryParams): Promise<ApiResponse<PageResponse<RagMarketDTO>>> {
  try {
    return await httpClient.get<ApiResponse<PageResponse<RagMarketDTO>>>(
      API_ENDPOINTS.MARKET,
      { params }
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "获取市场列表失败",
      data: { records: [], total: 0, size: 15, current: 1, pages: 0 },
      timestamp: Date.now()
    }
  }
}

/** 获取市场上的RAG版本列表（带Toast提示） */
export async function getMarketRagVersionsWithToast(params?: QueryParams): Promise<ApiResponse<PageResponse<RagMarketDTO>>> {
  const response = await getMarketRagVersions(params)
  
  if (response.code !== 200) {
    toast({
      title: "获取市场列表失败",
      description: response.message,
      variant: "destructive"
    })
  }
  
  return response
}

/** 安装RAG版本 */
export async function installRagVersion(data: InstallRagRequest): Promise<ApiResponse<UserRagDTO>> {
  try {
    return await httpClient.post<ApiResponse<UserRagDTO>>(
      `${API_ENDPOINTS.MARKET}/install`,
      data
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "安装失败",
      data: {} as UserRagDTO,
      timestamp: Date.now()
    }
  }
}

/** 安装RAG版本（带Toast提示） */
export async function installRagVersionWithToast(data: InstallRagRequest): Promise<ApiResponse<UserRagDTO>> {
  const response = await installRagVersion(data)
  
  if (response.code === 200) {
    toast({
      title: "安装成功",
      description: "RAG已成功安装",
    })
  } else {
    toast({
      title: "安装失败",
      description: response.message,
      variant: "destructive"
    })
  }
  
  return response
}

/** 卸载RAG版本 */
export async function uninstallRagVersion(ragVersionId: string): Promise<ApiResponse<void>> {
  try {
    return await httpClient.delete<ApiResponse<void>>(
      `${API_ENDPOINTS.MARKET}/uninstall/${ragVersionId}`
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "卸载失败",
      data: undefined,
      timestamp: Date.now()
    }
  }
}

/** 卸载RAG版本（带Toast提示） */
export async function uninstallRagVersionWithToast(ragVersionId: string): Promise<ApiResponse<void>> {
  const response = await uninstallRagVersion(ragVersionId)
  
  if (response.code === 200) {
    toast({
      title: "卸载成功",
      description: "RAG已成功卸载",
    })
  } else {
    toast({
      title: "卸载失败",
      description: response.message,
      variant: "destructive"
    })
  }
  
  return response
}

/** 获取用户安装的RAG列表 */
export async function getUserInstalledRags(params?: QueryParams): Promise<ApiResponse<PageResponse<UserRagDTO>>> {
  try {
    return await httpClient.get<ApiResponse<PageResponse<UserRagDTO>>>(
      `${API_ENDPOINTS.MARKET}/installed`,
      { params }
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "获取安装列表失败",
      data: { records: [], total: 0, size: 15, current: 1, pages: 0 },
      timestamp: Date.now()
    }
  }
}

/** 获取用户安装的RAG列表（带Toast提示） */
export async function getUserInstalledRagsWithToast(params?: QueryParams): Promise<ApiResponse<PageResponse<UserRagDTO>>> {
  const response = await getUserInstalledRags(params)
  
  if (response.code !== 200) {
    toast({
      title: "获取安装列表失败",
      description: response.message,
      variant: "destructive"
    })
  }
  
  return response
}

/** 获取用户安装的所有RAG（对话中使用） */
export async function getUserAllInstalledRags(): Promise<ApiResponse<UserRagDTO[]>> {
  try {
    return await httpClient.get<ApiResponse<UserRagDTO[]>>(
      `${API_ENDPOINTS.MARKET}/installed/all`
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "获取安装列表失败",
      data: [],
      timestamp: Date.now()
    }
  }
}


/** 检查RAG使用权限 */
export async function checkRagPermission(ragId?: string, ragVersionId?: string): Promise<ApiResponse<boolean>> {
  try {
    const params: any = {}
    if (ragId) params.ragId = ragId
    if (ragVersionId) params.ragVersionId = ragVersionId
    
    return await httpClient.get<ApiResponse<boolean>>(
      `${API_ENDPOINTS.MARKET}/permission/check`,
      { params }
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "权限检查失败",
      data: false,
      timestamp: Date.now()
    }
  }
}

/** 切换已安装RAG的版本 */
export async function switchRagVersion(userRagId: string, targetVersionId: string): Promise<ApiResponse<UserRagDTO>> {
  try {
    return await httpClient.put<ApiResponse<UserRagDTO>>(
      `${API_ENDPOINTS.MARKET}/installed/${userRagId}/switch-version?targetVersionId=${targetVersionId}`,
      {}
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "版本切换失败",
      data: {} as UserRagDTO,
      timestamp: Date.now()
    }
  }
}

/** 切换已安装RAG的版本（带Toast提示） */
export async function switchRagVersionWithToast(userRagId: string, targetVersionId: string): Promise<ApiResponse<UserRagDTO>> {
  const response = await switchRagVersion(userRagId, targetVersionId)
  
  if (response.code === 200) {
    toast({
      title: "版本切换成功",
      description: `已切换到版本 v${response.data.version}`,
    })
  } else {
    toast({
      title: "版本切换失败",
      description: response.message,
      variant: "destructive"
    })
  }
  
  return response
}

/** 获取已安装RAG的所有版本列表 */
export async function getInstalledRagVersions(userRagId: string): Promise<ApiResponse<UserRagDTO[]>> {
  try {
    return await httpClient.get<ApiResponse<UserRagDTO[]>>(
      `${API_ENDPOINTS.MARKET}/installed/${userRagId}/versions`
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "获取版本列表失败",
      data: [],
      timestamp: Date.now()
    }
  }
}

/** 获取已安装RAG的所有版本列表（带Toast提示） */
export async function getInstalledRagVersionsWithToast(userRagId: string): Promise<ApiResponse<UserRagDTO[]>> {
  const response = await getInstalledRagVersions(userRagId)
  
  if (response.code !== 200) {
    toast({
      title: "获取版本列表失败",
      description: response.message,
      variant: "destructive"
    })
  }
  
  return response
}

// ================================ 管理员审核相关接口 ================================

/** 获取待审核的RAG版本列表 */
export async function getPendingReviewVersions(params?: QueryParams): Promise<ApiResponse<PageResponse<RagVersionDTO>>> {
  try {
    return await httpClient.get<ApiResponse<PageResponse<RagVersionDTO>>>(
      `${API_ENDPOINTS.ADMIN_REVIEW}/pending`,
      { params }
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "获取待审核列表失败",
      data: { records: [], total: 0, size: 15, current: 1, pages: 0 },
      timestamp: Date.now()
    }
  }
}

/** 审核RAG版本 */
export async function reviewRagVersion(versionId: string, data: ReviewRagVersionRequest): Promise<ApiResponse<RagVersionDTO>> {
  try {
    return await httpClient.post<ApiResponse<RagVersionDTO>>(
      `${API_ENDPOINTS.ADMIN_REVIEW}/${versionId}`,
      data
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "审核失败",
      data: {} as RagVersionDTO,
      timestamp: Date.now()
    }
  }
}

/** 审核RAG版本（带Toast提示） */
export async function reviewRagVersionWithToast(versionId: string, data: ReviewRagVersionRequest): Promise<ApiResponse<RagVersionDTO>> {
  const response = await reviewRagVersion(versionId, data)
  
  if (response.code === 200) {
    toast({
      title: "审核成功",
      description: "审核结果已保存",
    })
  } else {
    toast({
      title: "审核失败",
      description: response.message,
      variant: "destructive"
    })
  }
  
  return response
}

/** 下架RAG版本 */
export async function removeRagVersion(versionId: string): Promise<ApiResponse<RagVersionDTO>> {
  try {
    return await httpClient.post<ApiResponse<RagVersionDTO>>(
      `${API_ENDPOINTS.ADMIN_REVIEW}/${versionId}/remove`
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "下架失败",
      data: {} as RagVersionDTO,
      timestamp: Date.now()
    }
  }
}

/** 下架RAG版本（带Toast提示） */
export async function removeRagVersionWithToast(versionId: string): Promise<ApiResponse<RagVersionDTO>> {
  const response = await removeRagVersion(versionId)
  
  if (response.code === 200) {
    toast({
      title: "下架成功",
      description: "RAG已成功下架",
    })
  } else {
    toast({
      title: "下架失败",
      description: response.message,
      variant: "destructive"
    })
  }
  
  return response
}

// ================================ 已安装RAG数据访问接口 ================================

/** 获取已安装RAG的文件列表 */
export async function getInstalledRagFiles(userRagId: string): Promise<ApiResponse<any[]>> {
  try {
    return await httpClient.get<ApiResponse<any[]>>(
      `${API_ENDPOINTS.MARKET}/installed/${userRagId}/files`
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "获取文件列表失败",
      data: [],
      timestamp: Date.now()
    }
  }
}

/** 获取已安装RAG的文件列表（带Toast提示） */
export async function getInstalledRagFilesWithToast(userRagId: string): Promise<ApiResponse<any[]>> {
  const response = await getInstalledRagFiles(userRagId)
  
  if (response.code !== 200) {
    toast({
      title: "获取文件列表失败",
      description: response.message,
      variant: "destructive"
    })
  }
  
  return response
}

/** 获取已安装RAG的文档列表 */
export async function getInstalledRagDocuments(userRagId: string): Promise<ApiResponse<any[]>> {
  try {
    return await httpClient.get<ApiResponse<any[]>>(
      `${API_ENDPOINTS.MARKET}/installed/${userRagId}/documents`
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "获取文档列表失败",
      data: [],
      timestamp: Date.now()
    }
  }
}

/** 获取已安装RAG的文档列表（带Toast提示） */
export async function getInstalledRagDocumentsWithToast(userRagId: string): Promise<ApiResponse<any[]>> {
  const response = await getInstalledRagDocuments(userRagId)
  
  if (response.code !== 200) {
    toast({
      title: "获取文档列表失败",
      description: response.message,
      variant: "destructive"
    })
  }
  
  return response
}

/** 获取已安装RAG特定文件的信息 */
export async function getInstalledRagFileInfo(userRagId: string, fileId: string): Promise<ApiResponse<any>> {
  try {
    return await httpClient.get<ApiResponse<any>>(
      `${API_ENDPOINTS.MARKET}/installed/${userRagId}/files/${fileId}/info`
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "获取文件信息失败",
      data: null,
      timestamp: Date.now()
    }
  }
}

/** 获取已安装RAG特定文件的文档列表 */
export async function getInstalledRagFileDocuments(userRagId: string, fileId: string): Promise<ApiResponse<any[]>> {
  try {
    return await httpClient.get<ApiResponse<any[]>>(
      `${API_ENDPOINTS.MARKET}/installed/${userRagId}/files/${fileId}/documents`
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "获取文档列表失败",
      data: [],
      timestamp: Date.now()
    }
  }
}

/** 获取已安装RAG特定文件的信息（带Toast提示） */
export async function getInstalledRagFileInfoWithToast(userRagId: string, fileId: string): Promise<ApiResponse<any>> {
  const response = await getInstalledRagFileInfo(userRagId, fileId)
  
  if (response.code !== 200) {
    toast({
      title: "获取文件信息失败",
      description: response.message,
      variant: "destructive"
    })
  }
  
  return response
}

/** 获取已安装RAG特定文件的文档列表（带Toast提示） */
export async function getInstalledRagFileDocumentsWithToast(userRagId: string, fileId: string): Promise<ApiResponse<any[]>> {
  const response = await getInstalledRagFileDocuments(userRagId, fileId)
  
  if (response.code !== 200) {
    toast({
      title: "获取文档列表失败",
      description: response.message,
      variant: "destructive"
    })
  }
  
  return response
}

/** 获取市场RAG的文件列表 */
export async function getMarketRagFiles(ragVersionId: string): Promise<ApiResponse<any[]>> {
  try {
    return await httpClient.get<ApiResponse<any[]>>(
      `${API_ENDPOINTS.MARKET}/${ragVersionId}/files`
    )
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "获取文件列表失败",
      data: [],
      timestamp: Date.now()
    }
  }
}

/** 获取市场RAG的文件列表（带Toast提示） */
export async function getMarketRagFilesWithToast(ragVersionId: string): Promise<ApiResponse<any[]>> {
  const response = await getMarketRagFiles(ragVersionId)
  
  if (response.code !== 200) {
    toast({
      title: "获取文件列表失败",
      description: response.message,
      variant: "destructive"
    })
  }
  
  return response
}

/** 获取市场RAG特定文件的文档单元 - 已禁用 */
// export async function getMarketRagFileDocuments(ragVersionId: string, fileId: string): Promise<ApiResponse<any[]>> {
//   try {
//     return await httpClient.get<ApiResponse<any[]>>(
//       `${API_ENDPOINTS.MARKET}/${ragVersionId}/files/${fileId}/documents`
//     )
//   } catch (error) {
//     return {
//       code: 500,
//       message: error instanceof Error ? error.message : "获取文档单元失败",
//       data: [],
//       timestamp: Date.now()
//     }
//   }
// }

/** 获取市场RAG特定文件的文档单元（带Toast提示） - 已禁用 */
// export async function getMarketRagFileDocumentsWithToast(ragVersionId: string, fileId: string): Promise<ApiResponse<any[]>> {
//   const response = await getMarketRagFileDocuments(ragVersionId, fileId)
  
//   if (response.code !== 200) {
//     toast({
//       title: "获取文档单元失败",
//       description: response.message,
//       variant: "destructive"
//     })
//   }
  
//   return response
// }