/**
 * 插件控制器
 * 用户使用插件
 */
package org.xhy.interfaces.api.portal.plugins;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xhy.application.plugins.dto.PluginDTO;
import org.xhy.application.plugins.service.PluginUseAppService;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.plugins.GetListReq;

import jakarta.validation.Valid;

/**
 * 插件控制器
 * 用户使用插件
 */
@RestController
@RequestMapping("/api/plugins")
public class PluginController {

    private final PluginUseAppService pluginUseAppService;

    public PluginController(PluginUseAppService pluginUseAppService) {
        this.pluginUseAppService = pluginUseAppService;
    }

    /**
     * 获取插件列表
     */
    @GetMapping
    public Result<List<PluginDTO>> listPlugins(@ModelAttribute @Valid GetListReq req) {
        return Result.success(pluginUseAppService.searchPlugins(req.getName(), req.getStatus(), req.getPage(), req.getSize()));
    }

    /**
     * 获取插件详情
     */
    @GetMapping("/{id}")
    public Result<PluginDTO> getPluginDetail(@PathVariable("id") String id) {
        return Result.success(pluginUseAppService.getPlugin(id));
    }
}
