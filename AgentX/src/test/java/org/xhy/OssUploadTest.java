package org.xhy;

import org.xhy.infrastructure.config.S3Properties;
import org.xhy.infrastructure.storage.OssUploadService;
import org.xhy.infrastructure.storage.OssUploadService.UploadCredential;

/** OSS上传凭证测试 测试生成前端直传OSS的上传凭证功能 */
public class OssUploadTest {

    public static void main(String[] args) {
        System.out.println("开始OSS上传凭证测试...");

        try {
            // 创建S3配置
            S3Properties s3Properties = new S3Properties();
            s3Properties.setEndpoint("");
            s3Properties.setAccessKey("");
            s3Properties.setSecretKey("");
            s3Properties.setBucketName("");
            s3Properties.setRegion("cn-beijing");
            s3Properties.setPathStyleAccess(false);

            System.out.println("OSS配置信息:");
            System.out.println("  端点: " + s3Properties.getEndpoint());
            System.out.println("  存储桶: " + s3Properties.getBucketName());
            System.out.println("  区域: " + s3Properties.getRegion());
            System.out.println("  访问密钥: " + s3Properties.getAccessKey().substring(0, 8) + "...");
            System.out.println();

            // 创建OSS上传服务
            OssUploadService ossUploadService = new OssUploadService(s3Properties);
            System.out.println("OSS上传服务创建成功");

            // 生成上传凭证
            String folder = "test-uploads";
            long maxFileSize = 10 * 1024 * 1024; // 10MB
            int expireMinutes = 30;

            System.out.println("生成上传凭证...");
            System.out.println("  文件夹: " + folder);
            System.out.println("  最大文件大小: " + (maxFileSize / 1024 / 1024) + "MB");
            System.out.println("  有效期: " + expireMinutes + "分钟");
            System.out.println();

            UploadCredential credential = ossUploadService.generateUploadCredential();

            System.out.println("上传凭证生成成功!");
            System.out.println("上传凭证信息:");
            System.out.println("  上传URL: " + credential.getUploadUrl());
            System.out.println("  访问密钥ID: " + credential.getAccessKeyId());
            System.out.println("  Policy: "
                    + credential.getPolicy().substring(0, Math.min(50, credential.getPolicy().length())) + "...");
            System.out.println("  签名: "
                    + credential.getSignature().substring(0, Math.min(20, credential.getSignature().length())) + "...");
            System.out.println("  对象键前缀: " + credential.getKeyPrefix());
            System.out.println("  访问URL前缀: " + credential.getAccessUrlPrefix());
            System.out.println("  过期时间: " + credential.getExpiration());
            System.out.println("  最大文件大小: " + credential.getMaxFileSize() + " 字节");
            System.out.println();

            // 模拟前端使用凭证的示例
            System.out.println("前端使用示例:");
            System.out.println("1. 获取上传凭证后，前端可以直接上传到OSS");
            System.out.println("2. 表单字段顺序（重要）:");
            System.out.println("   - key: " + credential.getKeyPrefix() + "your-file-name.jpg");
            System.out.println("   - policy: " + credential.getPolicy());
            System.out.println("   - OSSAccessKeyId: " + credential.getAccessKeyId());
            System.out.println("   - signature: " + credential.getSignature());
            System.out.println("   - file: [文件内容]");
            System.out.println("3. 上传成功后，文件可通过以下URL访问:");
            System.out.println("   " + credential.getAccessUrlPrefix() + "your-file-name.jpg");

            System.out.println("\n测试完成！");

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}