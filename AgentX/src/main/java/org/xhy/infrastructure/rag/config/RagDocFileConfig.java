package org.xhy.infrastructure.rag.config;

import java.util.Map;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.hash.HashInfo;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.constant.EmbeddingStatus;
import org.xhy.domain.rag.constant.FileInitializeStatus;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.repository.FileDetailRepository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;

/**
 * 文件上传服务服务配置
 * @author shilong.zang
 * @date 22:51 <br/>
 */
@Service
public class RagDocFileConfig implements FileRecorder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final FileDetailRepository fileDetailRepository;

    public RagDocFileConfig(FileDetailRepository fileDetailRepository) {
        this.fileDetailRepository = fileDetailRepository;
    }


    /**
     * 保存文件信息到数据库
     */
    @Override
    public boolean save(FileInfo info) {
        FileDetailEntity detail = null;
        try {
            detail = toFileDetailDO(info);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        //detail.setUserId(StpUtil.getLoginIdAsLong());
        fileDetailRepository.checkInsert(detail);

        info.setId(detail.getId());

        return Boolean.TRUE;
    }

    /**
     * 更新文件记录，可以根据文件 ID 或 URL 来更新文件记录，
     * 主要用在手动分片上传文件-完成上传，作用是更新文件信息
     */
    @Override
    public void update(FileInfo info) {
        FileDetailEntity detail = null;
        try {
            detail = toFileDetailDO(info);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        LambdaQueryWrapper<FileDetailEntity> qw = new LambdaQueryWrapper<FileDetailEntity>()
                .eq(detail.getUrl() != null, FileDetailEntity::getUrl, detail.getUrl())
                .eq(detail.getId() != null, FileDetailEntity::getId, detail.getId());
        fileDetailRepository.checkedUpdate(detail, qw);
    }

    /**
     * 根据 url 查询文件信息
     */
    @Override
    public FileInfo getByUrl(String url) {
        try {
            return toFileInfo(fileDetailRepository.selectOne(Wrappers.<FileDetailEntity>lambdaQuery().eq(FileDetailEntity::getUrl, url)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据 url 删除文件信息
     */
    @Override
    public boolean delete(String url) {
        fileDetailRepository.delete(Wrappers.lambdaQuery(FileDetailEntity.class).eq(FileDetailEntity::getUrl, url));
        return true;
    }

    @Override
    public void saveFilePart(FilePartInfo filePartInfo) {

    }

    @Override
    public void deleteFilePartByUploadId(String s) {

    }


    /**
     * 将 FileInfo 转为 FileDetailDO
     */
    public FileDetailEntity toFileDetailDO(FileInfo info) throws JsonProcessingException {
        FileDetailEntity detail = BeanUtil.copyProperties(
                info, FileDetailEntity.class, "metadata", "userMetadata", "thMetadata", "thUserMetadata", "attr", "hashInfo");

        detail.setDataSetId(info.getMetadata().get("dataset"));
        // 这里手动获 元数据 并转成 json 字符串，方便存储在数据库中
        detail.setMetadata(valueToJson(info.getMetadata()));
        detail.setUserMetadata(valueToJson(info.getUserMetadata()));
        detail.setThMetadata(valueToJson(info.getThMetadata()));
        detail.setThUserMetadata(valueToJson(info.getThUserMetadata()));
        // 这里手动获 取附加属性字典 并转成 json 字符串，方便存储在数据库中
        detail.setAttr(valueToJson(info.getAttr()));
        // 这里手动获 哈希信息 并转成 json 字符串，方便存储在数据库中
        detail.setHashInfo(valueToJson(info.getHashInfo()));
        detail.setIsEmbedding(EmbeddingStatus.UNINITIALIZED);
        detail.setIsInitialize(FileInitializeStatus.INITIALIZE_WAIT);
        return detail;
    }

    /**
     * 将 FileDetailDO 转为 FileInfo
     */
    public FileInfo toFileInfo(FileDetailEntity detail) throws JsonProcessingException {
        FileInfo info = BeanUtil.copyProperties(
                detail, FileInfo.class, "metadata", "userMetadata", "thMetadata", "thUserMetadata", "attr", "hashInfo");

        // 这里手动获取数据库中的 json 字符串 并转成 元数据，方便使用
        info.setMetadata(jsonToMetadata(detail.getMetadata()));
        info.setUserMetadata(jsonToMetadata(detail.getUserMetadata()));
        info.setThMetadata(jsonToMetadata(detail.getThMetadata()));
        info.setThUserMetadata(jsonToMetadata(detail.getThUserMetadata()));
        // 这里手动获取数据库中的 json 字符串 并转成 附加属性字典，方便使用
        info.setAttr(jsonToDict(detail.getAttr()));
        // 这里手动获取数据库中的 json 字符串 并转成 哈希信息，方便使用
        info.setHashInfo(jsonToHashInfo(detail.getHashInfo()));

        return info;
    }

    /**
     * 将指定值转换成 json 字符串
     */
    public String valueToJson(Object value) throws JsonProcessingException {
        if (value == null) {
            return null;
        }
        return objectMapper.writeValueAsString(value);
    }

    /**
     * 将 json 字符串转换成元数据对象
     */
    public Map<String, String> jsonToMetadata(String json) throws JsonProcessingException {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }

    /**
     * 将 json 字符串转换成字典对象
     */
    public Dict jsonToDict(String json) throws JsonProcessingException {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return objectMapper.readValue(json, Dict.class);
    }

    /**
     * 将 json 字符串转换成哈希信息对象
     */
    public HashInfo jsonToHashInfo(String json) throws JsonProcessingException {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return objectMapper.readValue(json, HashInfo.class);
    }
}