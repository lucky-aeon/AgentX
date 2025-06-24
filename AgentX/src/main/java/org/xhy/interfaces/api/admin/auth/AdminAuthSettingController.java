package org.xhy.interfaces.api.admin.auth;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xhy.application.auth.dto.AuthSettingDTO;
import org.xhy.application.auth.dto.UpdateAuthSettingRequest;
import org.xhy.application.auth.service.AuthSettingAppService;
import org.xhy.interfaces.api.common.Result;

import java.util.List;

/** 管理员认证配置控制器 */
@RestController
@RequestMapping("/admin/auth-settings")
public class AdminAuthSettingController {

    private final AuthSettingAppService authSettingAppService;

    public AdminAuthSettingController(AuthSettingAppService authSettingAppService) {
        this.authSettingAppService = authSettingAppService;
    }

    /** 获取所有认证配置
     * 
     * @return 认证配置列表 */
    @GetMapping
    public Result<List<AuthSettingDTO>> getAllAuthSettings() {
        List<AuthSettingDTO> settings = authSettingAppService.getAllAuthSettings();
        return Result.success(settings);
    }

    /** 根据ID获取认证配置
     * 
     * @param id 配置ID
     * @return 认证配置 */
    @GetMapping("/{id}")
    public Result<AuthSettingDTO> getAuthSettingById(@PathVariable String id) {
        AuthSettingDTO setting = authSettingAppService.getAuthSettingById(id);
        return Result.success(setting);
    }

    /** 切换认证配置启用状态
     * 
     * @param id 配置ID
     * @return 更新后的配置 */
    @PutMapping("/{id}/toggle")
    public Result<AuthSettingDTO> toggleAuthSetting(@PathVariable String id) {
        AuthSettingDTO setting = authSettingAppService.toggleAuthSetting(id);
        return Result.success(setting);
    }

    /** 更新认证配置
     * 
     * @param id 配置ID
     * @param request 更新请求
     * @return 更新后的配置 */
    @PutMapping("/{id}")
    public Result<AuthSettingDTO> updateAuthSetting(@PathVariable String id,
            @RequestBody @Validated UpdateAuthSettingRequest request) {
        AuthSettingDTO setting = authSettingAppService.updateAuthSetting(id, request);
        return Result.success(setting);
    }

    /** 删除认证配置
     * 
     * @param id 配置ID
     * @return 操作结果 */
    @DeleteMapping("/{id}")
    public Result<Void> deleteAuthSetting(@PathVariable String id) {
        authSettingAppService.deleteAuthSetting(id);
        return Result.success();
    }
}