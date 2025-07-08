package org.xhy.domain.rag.service;

import org.xhy.infrastructure.storage.StorageService;
import org.xhy.infrastructure.storage.UploadResult;

import java.io.IOException;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.repository.FileDetailRepository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

/** 文件详情领域服务
 * 
 * @author shilong.zang
 * @date 23:38 <br/> */
@Service
public class FileDetailDomainService {

    private final StorageService storageService;

    private final FileDetailRepository fileDetailRepository;


    public FileDetailDomainService(StorageService storageService, FileDetailRepository fileDetailRepository) {
        this.storageService = storageService;
        this.fileDetailRepository = fileDetailRepository;
    }

    public void upload(FileDetailEntity fileDetailEntity) {
        try {
            // 生成对象存储路径
            String objectKey = storageService.generateObjectKey(
                fileDetailEntity.getMultipartFile().getOriginalFilename(), 
                "rag-files"
            );
            
            // 上传文件
            UploadResult uploadResult = storageService.uploadStream(
                fileDetailEntity.getMultipartFile().getInputStream(),
                objectKey,
                fileDetailEntity.getMultipartFile().getSize()
            );
            
            // 更新实体信息
            fileDetailEntity.setUrl(uploadResult.getAccessUrl());
            fileDetailEntity.setSize(uploadResult.getFileSize());
            fileDetailEntity.setFilename(uploadResult.getStorageName());
            fileDetailEntity.setOriginalFilename(uploadResult.getOriginalName());
            fileDetailEntity.setPath(uploadResult.getFilePath());
            fileDetailEntity.setContentType(uploadResult.getContentType());
            fileDetailEntity.setPlatform(uploadResult.getBucketName());
            fileDetailEntity.setHashInfo(uploadResult.getMd5Hash());
            
            // 更新数据库
            fileDetailRepository.checkedUpdate(Wrappers.lambdaUpdate(FileDetailEntity.class)
                    .eq(FileDetailEntity::getId, fileDetailEntity.getId())
                    .set(FileDetailEntity::getDataSetId, fileDetailEntity.getDataSetId()));
                    
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }
}
