package org.xhy.interfaces.api.rag;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xhy.application.rag.service.FileService;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.rag.RagUploadRequest;

/**
 * @author shilong.zang
 * @date 10:50 <br/>
 */
@RestController
@RequestMapping("/rag/file")
public class RagUploadController {

    private final FileService fileService;

    public RagUploadController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 上传文件
     */
     @PostMapping("/upload")
     public Result<Void> upload(@ModelAttribute RagUploadRequest request) {
         fileService.upload(request);
         return Result.success();
     }
}
