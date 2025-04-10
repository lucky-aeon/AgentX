package org.xhy.domain.rag.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import org.xhy.infrastructure.entity.BaseEntity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * @author zang
 * @date 10:11 <br/>
 */
@TableName("file_detail")
public class FileDetailEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -1055107743652307804L;

    /**
     * 文件id
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 文件访问地址
     */
    private String url;

    /**
     * 文件大小，单位字节
     */
    private Long size;

    /**
     * 文件名称
     */
    private String filename;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 基础存储路径
     */
    private String basePath;

    /**
     * 存储路径
     */
    private String path;

    /**
     * 文件扩展名
     */
    private String ext;

    /**
     * MIME类型
     */
    private String contentType;

    /**
     * 存储平台
     */
    private String platform;

    /**
     * 缩略图访问路径
     */
    private String thUrl;

    /**
     * 缩略图名称
     */
    private String thFilename;

    /**
     * 缩略图大小，单位字节
     */
    private Long thSize;

    /**
     * 缩略图MIME类型
     */
    private String thContentType;

    /**
     * 文件所属对象id
     */
    private String objectId;

    /**
     * 文件所属对象类型，例如用户头像，评价图片
     */
    private String objectType;

    /**
     * 文件元数据
     */
    private String metadata;

    /**
     * 文件用户元数据
     */
    private String userMetadata;

    /**
     * 缩略图元数据
     */
    private String thMetadata;

    /**
     * 缩略图用户元数据
     */
    private String thUserMetadata;

    /**
     * 附加属性
     */
    private String attr;

    /**
     * 文件ACL
     */

    private String fileAcl;

    /**
     * 缩略图文件ACL
     */
    private String thFileAcl;

    /**
     * 哈希信息
     */
    private String hashInfo;

    /**
     * 上传ID，仅在手动分片上传时使用
     */
    private String uploadId;

    /**
     * 上传状态，仅在手动分片上传时使用，1：初始化完成，2：上传完成
     */
    private Integer uploadStatus;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 数据集id
     */
    private String dataSetId;

    /**
     * 总页数
     */
    private Integer filePageSize;

    /**
     * 是否进行初始化
     * @see org.xhy.domain.rag.constant.FileInitializeStatus
     */
    private String isInitialize;

    /**
     * 是否进行向量化
     * @see org.xhy.domain.rag.constant.EmbeddingStatus
     */
    private String isEmbedding;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getThUrl() {
        return thUrl;
    }

    public void setThUrl(String thUrl) {
        this.thUrl = thUrl;
    }

    public String getThFilename() {
        return thFilename;
    }

    public void setThFilename(String thFilename) {
        this.thFilename = thFilename;
    }

    public Long getThSize() {
        return thSize;
    }

    public void setThSize(Long thSize) {
        this.thSize = thSize;
    }

    public String getThContentType() {
        return thContentType;
    }

    public void setThContentType(String thContentType) {
        this.thContentType = thContentType;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getUserMetadata() {
        return userMetadata;
    }

    public void setUserMetadata(String userMetadata) {
        this.userMetadata = userMetadata;
    }

    public String getThMetadata() {
        return thMetadata;
    }

    public void setThMetadata(String thMetadata) {
        this.thMetadata = thMetadata;
    }

    public String getThUserMetadata() {
        return thUserMetadata;
    }

    public void setThUserMetadata(String thUserMetadata) {
        this.thUserMetadata = thUserMetadata;
    }

    public String getAttr() {
        return attr;
    }

    public void setAttr(String attr) {
        this.attr = attr;
    }

    public String getFileAcl() {
        return fileAcl;
    }

    public void setFileAcl(String fileAcl) {
        this.fileAcl = fileAcl;
    }

    public String getThFileAcl() {
        return thFileAcl;
    }

    public void setThFileAcl(String thFileAcl) {
        this.thFileAcl = thFileAcl;
    }

    public String getHashInfo() {
        return hashInfo;
    }

    public void setHashInfo(String hashInfo) {
        this.hashInfo = hashInfo;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public Integer getUploadStatus() {
        return uploadStatus;
    }

    public void setUploadStatus(Integer uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(String dataSetId) {
        this.dataSetId = dataSetId;
    }

    public Integer getFilePageSize() {
        return filePageSize;
    }

    public void setFilePageSize(Integer filePageSize) {
        this.filePageSize = filePageSize;
    }

    public String getIsInitialize() {
        return isInitialize;
    }

    public void setIsInitialize(String isInitialize) {
        this.isInitialize = isInitialize;
    }

    public String getIsEmbedding() {
        return isEmbedding;
    }

    public void setIsEmbedding(String isEmbedding) {
        this.isEmbedding = isEmbedding;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FileDetailEntity that = (FileDetailEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(url, that.url) && Objects.equals(size, that.size) && Objects.equals(filename, that.filename) && Objects.equals(originalFilename, that.originalFilename) && Objects.equals(basePath, that.basePath) && Objects.equals(path, that.path) && Objects.equals(ext, that.ext) && Objects.equals(contentType, that.contentType) && Objects.equals(platform, that.platform) && Objects.equals(thUrl, that.thUrl) && Objects.equals(thFilename, that.thFilename) && Objects.equals(thSize, that.thSize) && Objects.equals(thContentType, that.thContentType) && Objects.equals(objectId, that.objectId) && Objects.equals(objectType, that.objectType) && Objects.equals(metadata, that.metadata) && Objects.equals(userMetadata, that.userMetadata) && Objects.equals(thMetadata, that.thMetadata) && Objects.equals(thUserMetadata, that.thUserMetadata) && Objects.equals(attr, that.attr) && Objects.equals(fileAcl, that.fileAcl) && Objects.equals(thFileAcl, that.thFileAcl) && Objects.equals(hashInfo, that.hashInfo) && Objects.equals(uploadId, that.uploadId) && Objects.equals(uploadStatus, that.uploadStatus) && Objects.equals(userId, that.userId) && Objects.equals(dataSetId, that.dataSetId) && Objects.equals(filePageSize, that.filePageSize) && Objects.equals(isInitialize, that.isInitialize) && Objects.equals(isEmbedding, that.isEmbedding);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, url, size, filename, originalFilename, basePath, path, ext, contentType, platform, thUrl, thFilename, thSize, thContentType, objectId, objectType, metadata, userMetadata, thMetadata, thUserMetadata, attr, fileAcl, thFileAcl, hashInfo, uploadId, uploadStatus, userId, dataSetId, filePageSize, isInitialize, isEmbedding);
    }
}
