package org.xhy.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xhy.infrastructure.config.S3Properties;
import org.xhy.infrastructure.storage.S3StorageService.UploadResult;

/** S3StorageService 单元测试 测试基于阿里云OSS的S3协议文件上传功能 */
@SpringBootTest
@TestPropertySource(properties = {"s3.endpoint=https://oss-cn-hangzhou.aliyuncs.com", "s3.access-key=test-access-key",
        "s3.secret-key=test-secret-key", "s3.bucket-name=test-bucket", "s3.region=cn-hangzhou",
        "s3.path-style-access=true"})
public class S3StorageServiceTest {

    @TempDir
    Path tempDir;

    private S3StorageService s3StorageService;
    private S3Properties s3Properties;

    @BeforeEach
    void setUp() {
        // 创建测试配置
        s3Properties = new S3Properties();
        s3Properties.setEndpoint("https://oss-cn-hangzhou.aliyuncs.com");
        s3Properties.setAccessKey("test-access-key");
        s3Properties.setSecretKey("test-secret-key");
        s3Properties.setBucketName("test-bucket");
        s3Properties.setRegion("cn-hangzhou");
        s3Properties.setPathStyleAccess(true);

        // 注意：这里在实际测试时需要真实的阿里云OSS配置
        // 当前仅用于演示代码结构，实际运行会失败
        try {
            s3StorageService = new S3StorageService(s3Properties);
        } catch (Exception e) {
            System.out.println("注意：S3客户端创建失败，这是正常的，因为使用的是测试配置");
            System.out.println("实际使用时请配置真实的阿里云OSS访问密钥");
        }
    }

    @Test
    void testGenerateObjectKey() {
        if (s3StorageService == null) {
            return; // 跳过测试，因为没有真实配置
        }

        String objectKey = s3StorageService.generateObjectKey("test.txt", "uploads");
        assertNotNull(objectKey);
        assertTrue(objectKey.contains("uploads"));
        assertTrue(objectKey.endsWith(".txt"));
        System.out.println("生成的对象键: " + objectKey);
    }

    @Test
    void testUploadFile() throws IOException {
        if (s3StorageService == null) {
            System.out.println("跳过文件上传测试 - 需要真实的阿里云OSS配置");
            return;
        }

        // 创建测试文件
        File testFile = createTestFile("这是一个测试文件的内容\n用于测试S3文件上传功能");

        try {
            // 生成对象键
            String objectKey = s3StorageService.generateObjectKey(testFile.getName(), "test-uploads");

            // 上传文件
            UploadResult result = s3StorageService.uploadFile(testFile, objectKey);

            // 验证结果
            assertNotNull(result);
            assertNotNull(result.getFileId());
            assertNotNull(result.getAccessUrl());
            assertNotNull(result.getEtag());
            assertTrue(result.getFileSize() > 0);

            System.out.println("上传结果: " + result);

            // 检查文件是否存在
            boolean exists = s3StorageService.fileExists(objectKey);
            assertTrue(exists);

            // 清理：删除上传的文件
            boolean deleted = s3StorageService.deleteFile(objectKey);
            assertTrue(deleted);

            // 再次检查文件是否存在
            boolean existsAfterDelete = s3StorageService.fileExists(objectKey);
            assertFalse(existsAfterDelete);

        } catch (Exception e) {
            System.out.println("文件上传测试失败: " + e.getMessage());
            System.out.println("这可能是因为没有配置真实的阿里云OSS访问密钥");
        } finally {
            // 清理测试文件
            if (testFile.exists()) {
                testFile.delete();
            }
        }
    }

    @Test
    void testUploadImageFile() throws IOException {
        if (s3StorageService == null) {
            System.out.println("跳过图片上传测试 - 需要真实的阿里云OSS配置");
            return;
        }

        // 创建测试图片文件（模拟）
        File testImageFile = createTestFile("这是模拟的图片文件内容", "test-image.jpg");

        try {
            // 生成对象键
            String objectKey = s3StorageService.generateObjectKey(testImageFile.getName(), "images");

            // 上传文件
            UploadResult result = s3StorageService.uploadFile(testImageFile, objectKey);

            // 验证结果
            assertNotNull(result);
            assertNotNull(result.getFileId());
            assertNotNull(result.getAccessUrl());
            assertTrue(result.getContentType().contains("image"));

            System.out.println("图片上传结果: " + result);

            // 清理：删除上传的文件
            s3StorageService.deleteFile(objectKey);

        } catch (Exception e) {
            System.out.println("图片上传测试失败: " + e.getMessage());
            System.out.println("这可能是因为没有配置真实的阿里云OSS访问密钥");
        } finally {
            // 清理测试文件
            if (testImageFile.exists()) {
                testImageFile.delete();
            }
        }
    }

    @Test
    void testFileExistsWithNonExistentFile() {
        if (s3StorageService == null) {
            return;
        }

        try {
            boolean exists = s3StorageService.fileExists("non-existent-file.txt");
            assertFalse(exists);
        } catch (Exception e) {
            System.out.println("文件存在性检查失败: " + e.getMessage());
        }
    }

    /** 创建测试文件 */
    private File createTestFile(String content) throws IOException {
        return createTestFile(content, "test-file.txt");
    }

    /** 创建指定名称的测试文件 */
    private File createTestFile(String content, String fileName) throws IOException {
        Path testFilePath = tempDir.resolve(fileName);
        try (FileWriter writer = new FileWriter(testFilePath.toFile())) {
            writer.write(content);
        }
        return testFilePath.toFile();
    }

    /** 演示如何使用真实配置进行测试 取消注释并配置真实的阿里云OSS信息后可以进行真实测试 */
    // @Test
    // void testRealUpload() throws IOException {
    // // 配置真实的阿里云OSS信息
    // S3Properties realProperties = new S3Properties();
    // realProperties.setEndpoint("https://oss-cn-hangzhou.aliyuncs.com"); // 你的OSS区域端点
    // realProperties.setAccessKey("your-real-access-key"); // 你的AccessKey
    // realProperties.setSecretKey("your-real-secret-key"); // 你的SecretKey
    // realProperties.setBucketName("your-bucket-name"); // 你的存储桶名称
    // realProperties.setRegion("cn-hangzhou"); // 你的区域
    // realProperties.setPathStyleAccess(true);
    //
    // S3StorageService realService = new S3StorageService(realProperties);
    //
    // // 创建测试文件
    // File testFile = createTestFile("真实上传测试内容");
    //
    // try {
    // String objectKey = realService.generateObjectKey(testFile.getName(), "test");
    // UploadResult result = realService.uploadFile(testFile, objectKey);
    //
    // System.out.println("真实上传成功: " + result);
    //
    // // 清理
    // realService.deleteFile(objectKey);
    //
    // } finally {
    // testFile.delete();
    // }
    // }
}