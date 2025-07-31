package org.xhy;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

import java.io.ByteArrayInputStream;

/** x-file-storage 文件上传测试
 * 直接使用配置参数测试腾讯云COS上传功能 */
public class XFileStorageTest {

    public static void main(String[] args) {
        System.out.println("开始 x-file-storage 文件上传测试...");
        
        // 硬编码配置参数（来自application.yml）
        String accessKey = "";
        String secretKey = "";
        String endpoint = ""; // 修改为广州region，与domain匹配
        String bucketName = "";
        String basePath = "s3/";
        String domain = "";
        
        System.out.println("配置信息:");
        System.out.println("  端点: " + endpoint);
        System.out.println("  存储桶: " + bucketName);
        System.out.println("  基础路径: " + basePath);
        System.out.println("  访问域名: " + domain);
        System.out.println("  访问密钥: " + accessKey.substring(0, 8) + "...");
        System.out.println();
        
        try {
            // 创建AWS S3客户端
            BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, "ap-guangzhou"))
                    .withPathStyleAccessEnabled(false)
                    .build();
            
            System.out.println("S3客户端创建成功");
            
            // 创建测试文件内容
            String testContent = "这是一个测试文件内容，用于验证 x-file-storage 上传功能。\n测试时间: " + System.currentTimeMillis();
            byte[] fileBytes = testContent.getBytes("UTF-8");
            
            // 生成文件名
            String fileName = "test-" + System.currentTimeMillis() + ".txt";
            String objectKey = basePath + fileName;
            
            System.out.println("准备上传文件:");
            System.out.println("  文件名: " + fileName);
            System.out.println("  对象键: " + objectKey);
            System.out.println("  文件大小: " + fileBytes.length + " 字节");
            System.out.println("  文件内容预览: " + testContent.substring(0, Math.min(50, testContent.length())) + "...");
            System.out.println();
            
            // 设置对象元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(fileBytes.length);
            metadata.setContentType("text/plain");
            
            // 创建上传请求
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName, 
                    objectKey, 
                    new ByteArrayInputStream(fileBytes), 
                    metadata
            );
            
            // 执行上传
            System.out.println("开始上传文件...");
            PutObjectResult result = s3Client.putObject(putObjectRequest);
            
            System.out.println("\n文件上传成功！");
            System.out.println("上传结果信息:");
            System.out.println("  ETag: " + result.getETag());
            System.out.println("  版本ID: " + result.getVersionId());
            System.out.println("  对象键: " + objectKey);
            System.out.println("  文件访问URL: " + domain + objectKey);
            System.out.println("  存储桶: " + bucketName);
            System.out.println("  文件大小: " + fileBytes.length + " 字节");
            System.out.println();
            
            // 测试文件是否存在
            System.out.println("=== 验证文件是否存在 ===");
            boolean exists = s3Client.doesObjectExist(bucketName, objectKey);
            System.out.println("文件是否存在: " + exists);
            
            if (exists) {
                System.out.println("文件上传验证成功！");
                System.out.println("可以通过以下URL访问文件: " + domain + objectKey);
            } else {
                System.out.println("警告: 文件验证失败！");
            }
            
            System.out.println("\n测试完成！");
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}