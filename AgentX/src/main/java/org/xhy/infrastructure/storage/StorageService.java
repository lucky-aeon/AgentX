package org.xhy.infrastructure.storage;

import java.io.File;
import java.io.InputStream;

/**
 * 存储服务接口
 */
public interface StorageService {
    
    /**
     * 检查服务是否可用
     */
    boolean isAvailable();
    
    /**
     * 上传文件
     * 
     * @param file 本地文件
     * @param objectKey 对象存储中的文件路径
     * @return 上传结果信息
     */
    UploadResult uploadFile(File file, String objectKey);
    
    /**
     * 上传文件到指定桶
     * 
     * @param file 本地文件
     * @param objectKey 对象存储中的文件路径
     * @param bucketName 存储桶名称
     * @return 上传结果信息
     */
    UploadResult uploadFile(File file, String objectKey, String bucketName);
    
    /**
     * 上传输入流
     * 
     * @param inputStream 输入流
     * @param objectKey 对象存储中的文件路径
     * @param contentLength 内容长度
     * @return 上传结果信息
     */
    UploadResult uploadStream(InputStream inputStream, String objectKey, long contentLength);
    
    /**
     * 上传输入流到指定桶
     * 
     * @param inputStream 输入流
     * @param objectKey 对象存储中的文件路径
     * @param contentLength 内容长度
     * @param bucketName 存储桶名称
     * @return 上传结果信息
     */
    UploadResult uploadStream(InputStream inputStream, String objectKey, long contentLength, String bucketName);
    
    /**
     * 删除文件
     * 
     * @param objectKey 对象存储中的文件路径
     * @return 是否删除成功
     */
    boolean deleteFile(String objectKey);
    
    /**
     * 删除指定桶中的文件
     * 
     * @param objectKey 对象存储中的文件路径
     * @param bucketName 存储桶名称
     * @return 是否删除成功
     */
    boolean deleteFile(String objectKey, String bucketName);
    
    /**
     * 检查文件是否存在
     * 
     * @param objectKey 对象存储中的文件路径
     * @return 是否存在
     */
    boolean fileExists(String objectKey);
    
    /**
     * 检查指定桶中的文件是否存在
     * 
     * @param objectKey 对象存储中的文件路径
     * @param bucketName 存储桶名称
     * @return 是否存在
     */
    boolean fileExists(String objectKey, String bucketName);
    
    /**
     * 生成对象存储路径
     * 
     * @param originalFileName 原始文件名
     * @param folder 文件夹路径
     * @return 生成的对象路径
     */
    String generateObjectKey(String originalFileName, String folder);
} 