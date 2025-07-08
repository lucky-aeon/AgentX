package org.xhy.domain.rag.service;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.repository.FileDetailRepository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

/**
 * @author shilong.zang
 * @date 23:38 <br/>
 */
@Service
public class FileDetailDomainService {

    private final FileStorageService fileStorageService;

    private final FileDetailRepository fileDetailRepository;


    public FileDetailDomainService(FileStorageService fileStorageService, FileDetailRepository fileDetailRepository) {
        this.fileStorageService = fileStorageService;
        this.fileDetailRepository = fileDetailRepository;
    }

    public void upload(FileDetailEntity fileDetailEntity) {

        final FileInfo upload = fileStorageService.of(fileDetailEntity.getMultipartFile()).upload();

        fileDetailRepository.checkedUpdate(Wrappers.lambdaUpdate(FileDetailEntity.class)
                .eq(FileDetailEntity::getId, upload.getId())
                .set(FileDetailEntity::getDataSetId, fileDetailEntity.getDataSetId()));

    }
}
