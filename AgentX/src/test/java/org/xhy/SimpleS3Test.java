package org.xhy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.xhy.infrastructure.config.S3Properties;
import org.xhy.infrastructure.storage.S3StorageService;
import org.xhy.infrastructure.storage.UploadResult;

/** 简单的S3测试类 直接测试阿里云OSS的S3协议文件上传功能 */
public class SimpleS3Test {

    public static void main(String[] args) {
        System.out.println("开始S3文件上传测试...");

        try {
            // 创建S3配置
            S3Properties s3Properties = new S3Properties();
            s3Properties.setEndpoint("");
            s3Properties.setAccessKey("");
            s3Properties.setSecretKey("");
            s3Properties.setBucketName("");
            s3Properties.setRegion("cn-beijing");
            s3Properties.setPathStyleAccess(false);

            System.out.println("S3配置信息:");
            System.out.println("  端点: " + s3Properties.getEndpoint());
            System.out.println("  存储桶: " + s3Properties.getBucketName());
            System.out.println("  区域: " + s3Properties.getRegion());
            System.out.println("  路径样式访问: " + s3Properties.isPathStyleAccess() + " (false=虚拟主机样式, true=路径样式)");
            System.out.println("  访问密钥: " + s3Properties.getAccessKey().substring(0, 8) + "...");
            System.out.println();

            // 创建S3存储服务
            S3StorageService s3StorageService = new S3StorageService(s3Properties);
            System.out.println("S3客户端创建成功");

            // 创建测试文件
            File testFile = createTestFile();
            System.out.println("测试文件创建成功: " + testFile.getAbsolutePath());
            System.out.println("文件大小: " + testFile.length() + " 字节");

            // 生成对象键
            String objectKey = s3StorageService.generateObjectKey(testFile.getName(), "test-uploads");
            System.out.println("生成的对象键: " + objectKey);

            // 上传文件
            System.out.println("开始上传文件...");
            UploadResult result = s3StorageService.uploadFile(testFile, objectKey);

            System.out.println("文件上传成功!");
            System.out.println("上传结果:");
            System.out.println("  文件ID: " + result.getFileId());
            System.out.println("  原始文件名: " + result.getOriginalName());
            System.out.println("  存储名称: " + result.getStorageName());
            System.out.println("  文件大小: " + result.getFileSize() + " 字节");
            System.out.println("  内容类型: " + result.getContentType());
            System.out.println("  访问URL: " + result.getAccessUrl());
            System.out.println("  MD5值: " + result.getMd5Hash());
            System.out.println("  ETag: " + result.getEtag());
            System.out.println("  创建时间: " + result.getCreatedAt());

            // 检查文件是否存在
            boolean exists = s3StorageService.fileExists(objectKey);
            System.out.println("文件存在性检查: " + (exists ? "存在" : "不存在"));

            // 测试文件访问
            System.out.println("\n可以通过以下URL访问文件:");
            System.out.println(result.getAccessUrl());

            // 询问是否删除文件
            System.out.println("\n测试完成！");
            System.out.println("如需删除测试文件，可以调用: s3StorageService.deleteFile(\"" + objectKey + "\")");

            // 清理本地测试文件
            if (testFile.exists()) {
                testFile.delete();
                System.out.println("本地测试文件已清理");
            }

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** 创建测试文件 */
    private static File createTestFile() throws IOException {
        File testFile = File.createTempFile("s3_test_", ".txt");

        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write("这是一个S3文件上传测试文件\n");
            writer.write("测试时间: " + java.time.LocalDateTime.now() + "\n");
            writer.write("文件内容: Hello, 阿里云OSS!\n");
            writer.write("S3协议集成测试成功\n");
            writer.write("============================\n");
            writer.write("AgentX项目文件上传功能验证\n");
            writer.write("使用AWS S3 SDK访问阿里云对象存储\n");
            writer.write("支持文件上传、下载、删除等操作\n");
        }

        return testFile;
    }
}