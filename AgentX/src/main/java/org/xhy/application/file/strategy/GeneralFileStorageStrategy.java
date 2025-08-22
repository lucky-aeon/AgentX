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

/** 通用文件存储策略
 * 
 * 处理一般性文件的基本存储逻辑，不涉及复杂的业务处理
 * 
 * @author shilong.zang
 * @date 2024-12-09 */
@Component
public class GeneralFileStorageStrategy implements FileStorageStrategy {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FileDetailRepository fileDetailRepository;

    public GeneralFileStorageStrategy(FileDetailRepository fileDetailRepository) {
        this.fileDetailRepository = fileDetailRepository;
    }

    @Override
    public boolean save(FileInfo fileInfo) {
        try {
            FileDetailEntity fileDetailEntity = convertToFileDetailEntity(fileInfo);
            fileDetailRepository.checkInsert(fileDetailEntity);

            // 回写ID到原对象（第三方库需要）
            fileInfo.setId(fileDetailEntity.getId());

            return true;
        } catch (Exception e) {
            throw new RuntimeException("保存通用文件失败", e);
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
            throw new RuntimeException("更新通用文件失败", e);
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
            throw new RuntimeException("查询通用文件失败", e);
        }
    }

    @Override
    public boolean delete(String url) {
        FileDetailEntity fileDetailEntity = fileDetailRepository
                .selectOne(Wrappers.lambdaQuery(FileDetailEntity.class).eq(FileDetailEntity::getUrl, url));

        if (fileDetailEntity == null) {
            return false;
        }

        // 通用文件只删除基本记录，不处理复杂的关联数据
        fileDetailRepository.deleteById(fileDetailEntity.getId());
        return true;
    }

    /** 将FileInfo转换为FileDetailEntity */
    private FileDetailEntity convertToFileDetailEntity(FileInfo fileInfo) throws JsonProcessingException {
        FileDetailEntity fileDetailEntity = BeanUtil.copyProperties(fileInfo, FileDetailEntity.class, "metadata",
                "userMetadata", "thMetadata", "thUserMetadata", "attr", "hashInfo");

        // 设置基本信息
        if (fileInfo.getMetadata() != null) {
            fileDetailEntity.setDataSetId(fileInfo.getMetadata().get("dataset"));
            fileDetailEntity.setUserId(fileInfo.getMetadata().get("userid"));
        }

        // 转换元数据为JSON字符串
        fileDetailEntity.setMetadata(valueToJson(fileInfo.getMetadata()));
        fileDetailEntity.setUserMetadata(valueToJson(fileInfo.getUserMetadata()));
        fileDetailEntity.setThMetadata(valueToJson(fileInfo.getThMetadata()));
        fileDetailEntity.setThUserMetadata(valueToJson(fileInfo.getThUserMetadata()));

        // 转换附加属性和哈希信息
        fileDetailEntity.setAttr(valueToJson(fileInfo.getAttr()));
        fileDetailEntity.setHashInfo(valueToJson(fileInfo.getHashInfo()));

        // 通用文件不设置特殊的处理状态

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

        // 哈希信息处理需要特殊的类型转换
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