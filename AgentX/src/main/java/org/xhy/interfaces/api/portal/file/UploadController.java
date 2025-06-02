package org.xhy.interfaces.api.portal.file;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xhy.infrastructure.storage.OssUploadService;
import org.xhy.infrastructure.storage.OssUploadService.UploadCredential;
import org.xhy.interfaces.api.common.Result;

/** 文件上传控制器 提供前端直传OSS的上传凭证API */
@RestController
@RequestMapping("/upload")
public class UploadController {

    private final OssUploadService ossUploadService;

    public UploadController(OssUploadService ossUploadService) {
        this.ossUploadService = ossUploadService;
    }

    /** 获取上传凭证
     * 
     * @return 上传凭证 */
    @GetMapping("/credential")
    public Result<UploadCredential> getUploadCredential() {

        UploadCredential credential = ossUploadService.generateUploadCredential();

        return Result.success(credential);
    }

}