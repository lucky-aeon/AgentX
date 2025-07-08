package org.xhy.infrastructure.storage;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;
import org.xhy.infrastructure.config.OssProperties;

/** OSS上传服务 提供前端直传OSS的上传凭证生成功能 */
@Service
public class OssUploadService {

    private final OssProperties ossProperties;

    public OssUploadService(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
    }

    /** 生成前端直传OSS的上传凭证
     * 
     * @return 上传凭证信息 */
    public UploadCredential generateUploadCredential() {
        try {
            // 生成过期时间
            long expireTime = System.currentTimeMillis() + 60 * 1000;
            Date expiration = new Date(expireTime);

            // 生成对象键前缀
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String keyPrefix = "agent" + "/" + datePath + "/";

            // 构建Policy
            Map<String, Object> policy = new HashMap<>();
            policy.put("expiration", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(expiration));

            List<Object> conditions = new ArrayList<>();
            conditions.add(Map.of("bucket", ossProperties.getBucketName()));
            conditions.add(new String[]{"starts-with", "$key", keyPrefix});
            conditions.add(new Object[]{"content-length-range", 0, 10485760}); // 0到10MB
            policy.put("conditions", conditions);

            // 转换为JSON并Base64编码
            String policyJson = mapToJson(policy);
            String encodedPolicy = Base64.getEncoder().encodeToString(policyJson.getBytes("UTF-8"));

            // 生成签名
            String signature = generateSignature(encodedPolicy);

            // 构建上传URL
            String uploadUrl = "https://" + ossProperties.getBucketName() + "."
                    + ossProperties.getEndpoint().replace("https://", "");

            // 构建文件访问URL前缀
            String accessUrlPrefix = uploadUrl + "/" + keyPrefix;

            return new UploadCredential(uploadUrl, ossProperties.getAccessKey(), encodedPolicy, signature, keyPrefix,
                    accessUrlPrefix, expiration, 10485760);

        } catch (Exception e) {
            throw new RuntimeException("生成上传凭证失败", e);
        }
    }

    /** 生成STS临时凭证（如果需要更安全的方式）
     * 
     * @param durationSeconds 凭证有效期（秒）
     * @return STS凭证 */
    public StsCredential generateStsCredential(int durationSeconds) {
        // 注意：这里需要阿里云STS SDK，暂时返回固定格式
        // 实际使用时需要集成阿里云STS服务

        long expireTime = System.currentTimeMillis() + durationSeconds * 1000;

        return new StsCredential("STS.temp_access_key", // 实际应该从STS服务获取
                "temp_secret_key", // 实际应该从STS服务获取
                "security_token", // 实际应该从STS服务获取
                new Date(expireTime), ossProperties.getBucketName(), ossProperties.getEndpoint());
    }

    /** 验证上传回调签名（可选）
     * 
     * @param authorization 授权头
     * @param pubKeyUrl 公钥URL
     * @param requestBody 请求体
     * @return 是否验证通过 */
    public boolean verifyCallback(String authorization, String pubKeyUrl, String requestBody) {
        // 这里可以实现OSS回调签名验证逻辑
        // 具体实现需要根据阿里云OSS回调文档
        return true;
    }

    /** 生成签名 */
    private String generateSignature(String encodedPolicy) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec secretKeySpec = new SecretKeySpec(ossProperties.getSecretKey().getBytes("UTF-8"), "HmacSHA1");
        mac.init(secretKeySpec);
        byte[] signatureBytes = mac.doFinal(encodedPolicy.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    /** 简单的Map转JSON实现 */
    private String mapToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else if (value instanceof List) {
                json.append(listToJson((List<?>) value));
            } else {
                json.append(value);
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    /** 简单的List转JSON实现 */
    private String listToJson(List<?> list) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                json.append(",");
            }
            if (item instanceof String) {
                json.append("\"").append(item).append("\"");
            } else if (item instanceof String[]) {
                String[] array = (String[]) item;
                json.append("[");
                for (int i = 0; i < array.length; i++) {
                    if (i > 0)
                        json.append(",");
                    json.append("\"").append(array[i]).append("\"");
                }
                json.append("]");
            } else if (item instanceof Object[]) {
                // 处理Object数组，例如content-length-range
                Object[] array = (Object[]) item;
                json.append("[");
                for (int i = 0; i < array.length; i++) {
                    if (i > 0)
                        json.append(",");
                    if (array[i] instanceof String) {
                        json.append("\"").append(array[i]).append("\"");
                    } else {
                        json.append(array[i]); // 数字直接输出，不加引号
                    }
                }
                json.append("]");
            } else if (item instanceof Map) {
                json.append(mapToJson((Map<String, Object>) item));
            } else {
                json.append(item);
            }
            first = false;
        }
        json.append("]");
        return json.toString();
    }

    /** 上传凭证类 */
    public static class UploadCredential {
        private final String uploadUrl; // 上传地址
        private final String accessKeyId; // AccessKey ID
        private final String policy; // Base64编码的Policy
        private final String signature; // 签名
        private final String keyPrefix; // 对象键前缀
        private final String accessUrlPrefix; // 访问URL前缀
        private final Date expiration; // 过期时间
        private final long maxFileSize; // 最大文件大小

        public UploadCredential(String uploadUrl, String accessKeyId, String policy, String signature, String keyPrefix,
                String accessUrlPrefix, Date expiration, long maxFileSize) {
            this.uploadUrl = uploadUrl;
            this.accessKeyId = accessKeyId;
            this.policy = policy;
            this.signature = signature;
            this.keyPrefix = keyPrefix;
            this.accessUrlPrefix = accessUrlPrefix;
            this.expiration = expiration;
            this.maxFileSize = maxFileSize;
        }

        // Getters
        public String getUploadUrl() {
            return uploadUrl;
        }
        public String getAccessKeyId() {
            return accessKeyId;
        }
        public String getPolicy() {
            return policy;
        }
        public String getSignature() {
            return signature;
        }
        public String getKeyPrefix() {
            return keyPrefix;
        }
        public String getAccessUrlPrefix() {
            return accessUrlPrefix;
        }
        public Date getExpiration() {
            return expiration;
        }
        public long getMaxFileSize() {
            return maxFileSize;
        }
    }

    /** STS临时凭证类 */
    public static class StsCredential {
        private final String accessKeyId;
        private final String accessKeySecret;
        private final String securityToken;
        private final Date expiration;
        private final String bucketName;
        private final String endpoint;

        public StsCredential(String accessKeyId, String accessKeySecret, String securityToken, Date expiration,
                String bucketName, String endpoint) {
            this.accessKeyId = accessKeyId;
            this.accessKeySecret = accessKeySecret;
            this.securityToken = securityToken;
            this.expiration = expiration;
            this.bucketName = bucketName;
            this.endpoint = endpoint;
        }

        // Getters
        public String getAccessKeyId() {
            return accessKeyId;
        }
        public String getAccessKeySecret() {
            return accessKeySecret;
        }
        public String getSecurityToken() {
            return securityToken;
        }
        public Date getExpiration() {
            return expiration;
        }
        public String getBucketName() {
            return bucketName;
        }
        public String getEndpoint() {
            return endpoint;
        }
    }
}