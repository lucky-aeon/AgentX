// 文件上传服务
import { toast } from '@/hooks/use-toast'
import { httpClient } from '@/lib/http-client'

// OSS上传凭证接口
export interface OssUploadCredential {
  uploadUrl: string
  accessKeyId: string
  policy: string
  signature: string
  keyPrefix: string
  accessUrlPrefix: string
  expiration: string
  maxFileSize: number
}

// 后端响应格式
interface UploadCredentialResponse {
  code: number
  message: string
  data: OssUploadCredential
  timestamp: number
}

// 上传结果
export interface UploadResult {
  url: string
  fileName: string
  fileSize: number
  fileType: string
}

// 上传文件信息
export interface UploadFileInfo {
  file: File
  fileName: string
  fileType: string
  fileSize: number
}

/**
 * 获取OSS上传凭证
 */
export async function getUploadCredential(): Promise<OssUploadCredential> {
  try {
    const response = await httpClient.get<UploadCredentialResponse>('/upload/credential')
    
    if (response.code !== 200) {
      throw new Error(response.message || '获取上传凭证失败')
    }

    return response.data
  } catch (error) {
    console.error('获取上传凭证失败:', error)
    throw new Error(error instanceof Error ? error.message : '获取上传凭证失败')
  }
}

/**
 * 上传文件到OSS
 */
export async function uploadFileToOss(
  fileInfo: UploadFileInfo,
  credential: OssUploadCredential,
  onProgress?: (progress: number) => void
): Promise<UploadResult> {
  try {
    // 生成唯一文件名
    const timestamp = Date.now()
    const randomStr = Math.random().toString(36).substring(2, 8)
    const fileExtension = fileInfo.fileName.split('.').pop() || ''
    const uniqueFileName = `${timestamp}_${randomStr}.${fileExtension}`
    const objectKey = `${credential.keyPrefix}${uniqueFileName}`

    // 构建FormData
    const formData = new FormData()
    formData.append('key', objectKey)
    formData.append('policy', credential.policy)
    formData.append('OSSAccessKeyId', credential.accessKeyId)
    formData.append('signature', credential.signature)
    formData.append('file', fileInfo.file)

    // 创建XMLHttpRequest以支持进度回调
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest()

      // 进度回调
      if (onProgress) {
        xhr.upload.addEventListener('progress', (event) => {
          if (event.lengthComputable) {
            const progress = Math.round((event.loaded / event.total) * 100)
            onProgress(progress)
          }
        })
      }

      // 完成回调
      xhr.addEventListener('load', () => {
        if (xhr.status === 204) {
          // OSS上传成功返回204
          const fileUrl = `${credential.accessUrlPrefix}${uniqueFileName}`
          resolve({
            url: fileUrl,
            fileName: fileInfo.fileName,
            fileSize: fileInfo.fileSize,
            fileType: fileInfo.fileType
          })
        } else {
          reject(new Error(`上传失败: HTTP ${xhr.status}`))
        }
      })

      // 错误回调
      xhr.addEventListener('error', () => {
        reject(new Error('网络错误，上传失败'))
      })

      // 超时回调
      xhr.addEventListener('timeout', () => {
        reject(new Error('上传超时'))
      })

      // 设置超时时间（30秒）
      xhr.timeout = 30000

      // 发送请求
      xhr.open('POST', credential.uploadUrl)
      xhr.send(formData)
    })
  } catch (error) {
    console.error('上传文件失败:', error)
    throw new Error(error instanceof Error ? error.message : '上传文件失败')
  }
}

/**
 * 批量上传文件
 */
export async function uploadMultipleFiles(
  files: UploadFileInfo[],
  onProgress?: (fileIndex: number, progress: number) => void,
  onFileComplete?: (fileIndex: number, result: UploadResult) => void,
  onError?: (fileIndex: number, error: Error) => void
): Promise<UploadResult[]> {
  try {
    // 获取上传凭证
    const credential = await getUploadCredential()
    
    // 检查文件大小
    const oversizedFiles = files.filter(file => file.fileSize > credential.maxFileSize * 1024)
    if (oversizedFiles.length > 0) {
      throw new Error(`以下文件超过大小限制(${credential.maxFileSize}KB): ${oversizedFiles.map(f => f.fileName).join(', ')}`)
    }

    const results: UploadResult[] = []
    
    // 逐个上传文件
    for (let i = 0; i < files.length; i++) {
      try {
        const result = await uploadFileToOss(
          files[i],
          credential,
          (progress) => onProgress?.(i, progress)
        )
        
        results.push(result)
        onFileComplete?.(i, result)
      } catch (error) {
        const uploadError = error instanceof Error ? error : new Error('上传失败')
        onError?.(i, uploadError)
        throw uploadError
      }
    }

    return results
  } catch (error) {
    console.error('批量上传失败:', error)
    throw error
  }
}

/**
 * 简化的单文件上传接口
 */
export async function uploadSingleFile(
  file: File,
  onProgress?: (progress: number) => void
): Promise<UploadResult> {
  const fileInfo: UploadFileInfo = {
    file,
    fileName: file.name,
    fileType: file.type,
    fileSize: file.size
  }

  try {
    const credential = await getUploadCredential()
    
    // 检查文件大小
    if (file.size > credential.maxFileSize * 1024) {
      throw new Error(`文件 ${file.name} 超过大小限制(${credential.maxFileSize}KB)`)
    }

    return await uploadFileToOss(fileInfo, credential, onProgress)
  } catch (error) {
    console.error('单文件上传失败:', error)
    throw error
  }
} 