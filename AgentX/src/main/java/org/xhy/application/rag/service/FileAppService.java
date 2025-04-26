package org.xhy.application.rag.service;


import org.springframework.stereotype.Service;
import org.xhy.application.rag.assembler.FileAssembler;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.service.FileDetailDomainService;
import org.xhy.interfaces.dto.rag.RagUploadRequest;

/**
 * @author shilong.zang
 * @date 15:43 <br/>
 */

@Service
public class FileAppService {

    private final FileDetailDomainService fileDetailService;

    public FileAppService(FileDetailDomainService fileDetailService) {
        this.fileDetailService = fileDetailService;
    }


    public void upload(RagUploadRequest request) {

        final FileDetailEntity fileDetailEntity = FileAssembler.toEntity(request);

        fileDetailService.upload(fileDetailEntity);
    }
}
