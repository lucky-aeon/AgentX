package org.xhy.application.file.strategy;

import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.stereotype.Component;
import org.xhy.domain.file.constant.FileTypeEnum;
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

/** 头像文件存储策略
 * 
 * 处理用户头像文件的存储逻辑，包括图片处理相关的业务
 * 
 * @author shilong.zang
 * @date 2024-12-09 */
@Component
public class AvatarFileStorageStrategy implements FileStorageStrategy {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FileDetailRepository fileDetailRepository;

    public AvatarFileStorageStrategy(FileDetailRepository fileDetailRepository) {
        this.fileDetailRepository = fileDetailRepository;
    }

    @Override
    public boolean save(FileInfo fileInfo) {
        try {
            FileDetailEntity fileDetailEntity = convertToFileDetailEntity(fileInfo);
            fileDetailRepository.checkInsert(fileDetailEntity);

            // 回写ID到原对象（第三方库需要）
            fileInfo.setId(fileDetailEntity.getId());

            // TODO: 可以在这里添加头像相关的业务逻辑
            // 例如：图片压缩、尺寸调整、格式转换等

            return true;
        } catch (Exception e) {
            throw new RuntimeException("保存头像文件失败", e);
        }
    }

    @Override
    public void update(FileInfo fileInfo) {
        try {
            FileDetailEntity fileDetailEntity = convertToFileDetailEntity(fileInfo);

            LambdaQueryWrapper<FileDetailEntity> queryWrapper = new LambdaQueryWrapper<FileDetailEntity>()
                    .eq(fileDetailEntity.getUrl() != null, FileDetailEntity::getUrl, fileDetailEntity.getUrl())
                    .eq(fileDetailEntity.getId() != null, FileDetailEntity::getId, fileDetailEntity.getId());

            fileDetailRepository.checkedUpdate(fileDetailEntity, queryWrapper);
        } catch (Exception e) {
            throw new RuntimeException("更新头像文件失败", e);
        }
    }

    @Override
    public FileInfo getByUrl(String url) {
        try {
            FileDetailEntity fileDetailEntity = fileDetailRepository
                    .selectOne(Wrappers.<FileDetailEntity>lambdaQuery().eq(FileDetailEntity::getUrl, url));

            if (fileDetailEntity == null) {
                return null;
            }

            return convertToFileInfo(fileDetailEntity);
        } catch (Exception e) {
            throw new RuntimeException("查询头像文件失败", e);
        }
    }

    @Override
    public boolean delete(String url) {
        FileDetailEntity fileDetailEntity = fileDetailRepository
                .selectOne(Wrappers.lambdaQuery(FileDetailEntity.class).eq(FileDetailEntity::getUrl, url));

        if (fileDetailEntity == null) {
            return false;
        }

        // 头像文件删除：可能需要清理用户关联信息
        fileDetailRepository.deleteById(fileDetailEntity.getId());

        // TODO: 可以在这里添加头像删除的业务逻辑
        // 例如：更新用户表中的头像字段、清理缓存等

        return true;
    }

    /** 将FileInfo转换为FileDetailEntity */
    private FileDetailEntity convertToFileDetailEntity(FileInfo fileInfo) throws JsonProcessingException {
        FileDetailEntity fileDetailEntity = BeanUtil.copyProperties(fileInfo, FileDetailEntity.class, "metadata",
                "userMetadata", "thMetadata", "thUserMetadata", "attr", "hashInfo");

        // 设置头像相关的业务信息
        if (fileInfo.getMetadata() != null) {
            fileDetailEntity.setUserId(fileInfo.getMetadata().get("userid"));
            // 头像文件通常不需要dataset
        }

        // 转换元数据为JSON字符串
        fileDetailEntity.setMetadata(valueToJson(fileInfo.getMetadata()));
        fileDetailEntity.setUserMetadata(valueToJson(fileInfo.getUserMetadata()));
        fileDetailEntity.setThMetadata(valueToJson(fileInfo.getThMetadata()));
        fileDetailEntity.setThUserMetadata(valueToJson(fileInfo.getThUserMetadata()));

        // 转换附加属性和哈希信息
        fileDetailEntity.setAttr(valueToJson(fileInfo.getAttr()));
        fileDetailEntity.setHashInfo(valueToJson(fileInfo.getHashInfo()));

        // 头像文件可以设置特定的状态或标识

        return fileDetailEntity;
    }

    /** 将FileDetailEntity转换为FileInfo */
    private FileInfo convertToFileInfo(FileDetailEntity fileDetailEntity) throws JsonProcessingException {
        FileInfo fileInfo = BeanUtil.copyProperties(fileDetailEntity, FileInfo.class, "metadata", "userMetadata",
                "thMetadata", "thUserMetadata", "attr", "hashInfo");

        // 转换JSON字符串为对象
        fileInfo.setMetadata(jsonToMetadata(fileDetailEntity.getMetadata()));
        fileInfo.setUserMetadata(jsonToMetadata(fileDetailEntity.getUserMetadata()));
        fileInfo.setThMetadata(jsonToMetadata(fileDetailEntity.getThMetadata()));
        fileInfo.setThUserMetadata(jsonToMetadata(fileDetailEntity.getThUserMetadata()));
        fileInfo.setAttr(jsonToDict(fileDetailEntity.getAttr()));

        // 哈希信息处理
        if (StrUtil.isNotBlank(fileDetailEntity.getHashInfo())) {
            fileInfo.setHashInfo(objectMapper.readValue(fileDetailEntity.getHashInfo(),
                    org.dromara.x.file.storage.core.hash.HashInfo.class));
        }

        return fileInfo;
    }

    /** 将对象转换为JSON字符串 */
    private String valueToJson(Object value) throws JsonProcessingException {
        if (value == null) {
            return null;
        }
        return objectMapper.writeValueAsString(value);
    }

    /** 将JSON字符串转换为元数据Map */
    private java.util.Map<String, String> jsonToMetadata(String json) throws JsonProcessingException {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }

    /** 将JSON字符串转换为字典对象 */
    private Dict jsonToDict(String json) throws JsonProcessingException {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return objectMapper.readValue(json, Dict.class);
    }
}