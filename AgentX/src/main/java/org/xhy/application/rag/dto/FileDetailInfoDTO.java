package org.xhy.application.rag.dto;

/**
 * 文件详细信息DTO（包含文件路径）
 * 
 * @author shilong.zang
 */
public class FileDetailInfoDTO {
    
    /**
     * 文件ID
     */
    private String fileId;
    
    /**
     * 原始文件名
     */
    private String originalFilename;
    
    /**
     * 文件访问地址
     */
    private String url;
    
    /**
     * 文件存储路径
     */
    private String path;
    
    /**
     * 基础存储路径
     */
    private String basePath;
    
    /**
     * 文件大小
     */
    private Long size;
    
    /**
     * 文件扩展名
     */
    private String ext;
    
    /**
     * 总页数
     */
    private Integer filePageSize;
    
    /**
     * 数据集ID
     */
    private String dataSetId;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public Integer getFilePageSize() {
        return filePageSize;
    }

    public void setFilePageSize(Integer filePageSize) {
        this.filePageSize = filePageSize;
    }

    public String getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(String dataSetId) {
        this.dataSetId = dataSetId;
    }
}